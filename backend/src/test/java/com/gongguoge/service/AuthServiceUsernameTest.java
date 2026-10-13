package com.gongguoge.service;

import com.gongguoge.common.exception.BusinessException;
import com.gongguoge.common.util.JwtUtil;
import com.gongguoge.dto.LoginRequest;
import com.gongguoge.dto.RegisterRequest;
import com.gongguoge.mapper.SmsLogMapper;
import com.gongguoge.mapper.UserMapper;
import com.gongguoge.pojo.User;
import com.gongguoge.vo.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 「用户名 + 密码」通道单元测试。
 *
 * <p>这条通道的存在的意义是：手机号验证码通道要求用户必须有手机号、且每条短信都花钱，
 * 而申请软著 / App 备案需要向审核方提供**与开发者本人手机号无关**的测试账号。</p>
 *
 * <p>重点验证：注册不发短信、密码被真正加密、弱密码被拒、登录的三种失败分支，
 * 以及手机号通道未被改坏（回归）。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("用户名 + 密码通道")
class AuthServiceUsernameTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private SmsLogMapper smsLogMapper;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private SmsService smsService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userMapper, smsLogMapper, redisTemplate, jwtUtil, smsService);
        ReflectionTestUtils.setField(authService, "smsEnabled", false);
        ReflectionTestUtils.setField(authService, "mockSmsCode", "123456");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // 用户名账号没有手机号，phone 是 null —— 必须用 any() 而不是 anyString()
        when(jwtUtil.generateToken(any(), any())).thenReturn("test-token");
    }

    private RegisterRequest usernameRegister(String username, String password) {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setPassword(password);
        return req;
    }

    private LoginRequest usernameLogin(String username, String password) {
        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);
        return req;
    }

    // ---------------- 注册 ----------------

    @Test
    @DisplayName("用户名注册成功，且密码经过 BCrypt 加密后入库")
    void registerByUsernameHashesPassword() {
        when(userMapper.selectCount(any())).thenReturn(0L);

        LoginVO vo = authService.register(usernameRegister("test_reviewer", "Ggg@2026"));

        assertNotNull(vo);
        assertEquals("test-token", vo.getToken());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User saved = captor.getValue();
        assertEquals("test_reviewer", saved.getUsername());
        assertNull(saved.getPhone(), "用户名注册的账号不应有手机号");
        assertNotEquals("Ggg@2026", saved.getPassword(), "密码不能明文入库");
        assertTrue(new BCryptPasswordEncoder().matches("Ggg@2026", saved.getPassword()),
                "入库的应是可校验的 BCrypt 密文");
        assertEquals(1, saved.getStatus());
    }

    @Test
    @DisplayName("用户名注册全程不触发短信（不花钱、与手机号无关）")
    void registerByUsernameNeverSendsSms() {
        when(userMapper.selectCount(any())).thenReturn(0L);

        authService.register(usernameRegister("no_sms_user", "Ggg@2026"));

        verify(smsService, never()).sendCode(any(), any());
        verify(smsLogMapper, never()).insert(any());
    }

    @Test
    @DisplayName("用户名已被占用时拒绝注册")
    void registerByUsernameRejectsDuplicate() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(usernameRegister("taken_name", "Ggg@2026")));
        assertTrue(ex.getMessage().contains("已被占用"));
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("纯数字密码被拒（弱密码下限）")
    void registerByUsernameRejectsNumericPassword() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(usernameRegister("weak_user", "12345678")));
        assertTrue(ex.getMessage().contains("纯数字"));
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("给了用户名却不给密码：明确报错，不落库")
    void registerByUsernameWithoutPasswordFails() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(usernameRegister("nopass", null)));
        assertTrue(ex.getMessage().contains("密码"));
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("既没有用户名也没有手机号：给出可操作的报错")
    void registerWithoutAnyChannelFails() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.register(new RegisterRequest()));
        assertTrue(ex.getMessage().contains("用户名") || ex.getMessage().contains("手机号"));
        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("手机号通道缺验证码时被拦下（回归）")
    void registerByPhoneStillRequiresCode() {
        RegisterRequest req = new RegisterRequest();
        req.setPhone("13800000009");
        req.setPassword("Ggg@2026");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.register(req));
        assertTrue(ex.getMessage().contains("验证码"));
        verify(smsService, never()).sendCode(any(), any());
    }

    // ---------------- 登录 ----------------

    @Test
    @DisplayName("用户名 + 正确密码可登录")
    void loginByUsernameSucceeds() {
        User user = existingUser("reviewer01", "Ggg@2026");
        when(userMapper.selectOne(any())).thenReturn(user);

        LoginVO vo = authService.login(usernameLogin("reviewer01", "Ggg@2026"));

        assertEquals("test-token", vo.getToken());
        assertEquals("reviewer01", vo.getUserInfo().getUsername());
        assertNull(vo.getUserInfo().getPhone());
    }

    @Test
    @DisplayName("用户名登录：密码错误")
    void loginByUsernameWrongPasswordFails() {
        when(userMapper.selectOne(any())).thenReturn(existingUser("reviewer01", "Ggg@2026"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(usernameLogin("reviewer01", "WrongPass1")));
        assertTrue(ex.getMessage().contains("账号或密码错误"));
    }

    @Test
    @DisplayName("用户名登录：账号不存在时与密码错误返回同一提示（不泄露账号是否存在）")
    void loginByUsernameUnknownUserGivesSameMessage() {
        when(userMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(usernameLogin("nobody_here", "Ggg@2026")));
        assertEquals("账号或密码错误", ex.getMessage());
    }

    @Test
    @DisplayName("用户名登录：账号被禁用")
    void loginByUsernameDisabledAccountFails() {
        User user = existingUser("banned_user", "Ggg@2026");
        user.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(usernameLogin("banned_user", "Ggg@2026")));
        assertTrue(ex.getMessage().contains("禁用"));
    }

    @Test
    @DisplayName("用户名登录：账号没有密码（异常数据）时拒绝，不抛 NPE")
    void loginByUsernameWithoutStoredPasswordFails() {
        User user = existingUser("legacy_user", null);
        when(userMapper.selectOne(any())).thenReturn(user);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(usernameLogin("legacy_user", "Ggg@2026")));
        assertTrue(ex.getMessage().contains("账号或密码错误"));
    }

    @Test
    @DisplayName("用户名登录不给密码：明确报错")
    void loginByUsernameWithoutPasswordFails() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(usernameLogin("reviewer01", null)));
        assertTrue(ex.getMessage().contains("密码"));
    }

    @Test
    @DisplayName("既没有用户名也没有手机号：给出可操作的报错")
    void loginWithoutAnyChannelFails() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest()));
        assertTrue(ex.getMessage().contains("用户名") || ex.getMessage().contains("手机号"));
    }

    @Test
    @DisplayName("回归：手机号 + 密码登录仍然正常")
    void phonePasswordLoginStillWorks() {
        User user = new User();
        user.setPhone("13800000001");
        user.setPassword(new BCryptPasswordEncoder().encode("Ggg@2026"));
        user.setStatus(1);
        when(userMapper.selectOne(any())).thenReturn(user);

        LoginRequest req = new LoginRequest();
        req.setPhone("13800000001");
        req.setPassword("Ggg@2026");

        LoginVO vo = authService.login(req);
        assertEquals("test-token", vo.getToken());
        assertEquals("13800000001", vo.getUserInfo().getPhone());
    }

    private User existingUser(String username, String rawPassword) {
        User user = new User();
        user.setId(100L);
        user.setUsername(username);
        user.setNickname("测试账号");
        user.setStatus(1);
        if (rawPassword != null) {
            user.setPassword(new BCryptPasswordEncoder().encode(rawPassword));
        }
        return user;
    }
}
