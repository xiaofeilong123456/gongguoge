package com.gongguoge.service;

import com.gongguoge.common.exception.BusinessException;
import com.gongguoge.common.util.JwtUtil;
import com.gongguoge.dto.LoginRequest;
import com.gongguoge.dto.SmsCodeRequest;
import com.gongguoge.mapper.SmsLogMapper;
import com.gongguoge.mapper.UserMapper;
import com.gongguoge.pojo.User;
import com.gongguoge.vo.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 认证安全相关单元测试。
 *
 * <p>用 Mockito 隔离 Redis 与数据库，只验证业务判定逻辑：
 * 验证码场景隔离、错误码拒绝、验证成功后不可重放（一次性消费）、
 * 发送频率限制、未注册手机号自动注册。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceSmsTest {

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
        // mock 验证码现在来自配置（不再硬编码），测试里显式注入
        ReflectionTestUtils.setField(authService, "mockSmsCode", "123456");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(jwtUtil.generateToken(any(), anyString())).thenReturn("test-token");
    }

    private LoginRequest loginRequest(String phone, String code) {
        LoginRequest req = new LoginRequest();
        req.setPhone(phone);
        req.setCode(code);
        return req;
    }

    private void givenExistingUser(String phone) {
        User user = new User();
        user.setId(1L);
        user.setPhone(phone);
        user.setNickname("测试用户");
        user.setStatus(1);
        when(userMapper.selectOne(any())).thenReturn(user);
    }

    @Test
    @DisplayName("验证码正确时登录成功")
    void loginSucceedsWithCorrectCode() {
        String phone = "13800000001";
        givenExistingUser(phone);
        when(valueOps.get("gongguoge:sms:login:" + phone)).thenReturn("123456");

        LoginVO vo = authService.login(loginRequest(phone, "123456"));

        assertNotNull(vo);
        assertEquals("test-token", vo.getToken());
        assertEquals(phone, vo.getUserInfo().getPhone());
    }

    @Test
    @DisplayName("验证码错误时登录被拒")
    void loginFailsWithWrongCode() {
        String phone = "13800000002";
        when(valueOps.get("gongguoge:sms:login:" + phone)).thenReturn("123456");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest(phone, "000000")));
        assertTrue(ex.getMessage().contains("验证码"), "异常信息应提示验证码问题");
    }

    @Test
    @DisplayName("验证码不存在（已过期或未发送）时登录被拒")
    void loginFailsWhenCodeAbsent() {
        String phone = "13800000003";
        when(valueOps.get("gongguoge:sms:login:" + phone)).thenReturn(null);

        assertThrows(BusinessException.class, () -> authService.login(loginRequest(phone, "123456")));
    }

    @Test
    @DisplayName("验证成功后验证码立即被删除（不可重放）")
    void codeIsConsumedAfterSuccess() {
        String phone = "13800000004";
        givenExistingUser(phone);
        when(valueOps.get("gongguoge:sms:login:" + phone)).thenReturn("123456");

        authService.login(loginRequest(phone, "123456"));

        verify(redisTemplate, times(1)).delete("gongguoge:sms:login:" + phone);
    }

    @Test
    @DisplayName("场景隔离：register 场景的码不能用于 login 登录")
    void codeIsScopedByScene() {
        String phone = "13800000005";
        // 只存在 register 场景的码
        when(valueOps.get("gongguoge:sms:register:" + phone)).thenReturn("123456");
        when(valueOps.get("gongguoge:sms:login:" + phone)).thenReturn(null);

        assertThrows(BusinessException.class, () -> authService.login(loginRequest(phone, "123456")));
        // 登录失败不应消费掉 register 场景的码
        verify(redisTemplate, never()).delete("gongguoge:sms:register:" + phone);
    }

    @Test
    @DisplayName("未注册手机号在验证码登录时自动注册")
    void unregisteredPhoneAutoRegisters() {
        String phone = "13800000006";
        when(userMapper.selectOne(any())).thenReturn(null);
        when(valueOps.get("gongguoge:sms:login:" + phone)).thenReturn("123456");

        LoginVO vo = authService.login(loginRequest(phone, "123456"));

        assertNotNull(vo);
        verify(userMapper, times(1)).insert(any(User.class));
    }

    @Test
    @DisplayName("自动注册时使用调用方传入的昵称（供测试脚本标记账号）")
    void autoRegisterUsesProvidedNickname() {
        String phone = "13800000007";
        when(userMapper.selectOne(any())).thenReturn(null);
        when(valueOps.get("gongguoge:sms:login:" + phone)).thenReturn("123456");

        LoginRequest req = loginRequest(phone, "123456");
        req.setNickname("测试账号");
        authService.login(req);

        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertEquals("测试账号", captor.getValue().getNickname());
    }

    @Test
    @DisplayName("被禁用账号登录被拒（403）")
    void disabledUserIsRejected() {
        String phone = "13800000008";
        User user = new User();
        user.setId(9L);
        user.setPhone(phone);
        user.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(valueOps.get("gongguoge:sms:login:" + phone)).thenReturn("123456");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest(phone, "123456")));
        assertEquals(403, ex.getCode());
    }

    @Test
    @DisplayName("发送频率限制：1 分钟内重复发码被拒")
    void rateLimitBlocksSecondSend() {
        String phone = "13800000009";
        SmsCodeRequest req = new SmsCodeRequest();
        req.setPhone(phone);
        req.setScene("login");
        when(valueOps.get("gongguoge:sms:limit:login:" + phone)).thenReturn("1");

        BusinessException ex = assertThrows(BusinessException.class, () -> authService.sendSmsCode(req));
        assertTrue(ex.getMessage().contains("频繁"), "异常信息应提示发送过于频繁");
        verify(smsService, never()).sendCode(anyString(), anyString());
    }

    @Test
    @DisplayName("首次发码：写入验证码（5 分钟）与限流键（1 分钟）")
    void firstSendWritesCodeAndLimit() {
        String phone = "13800000010";
        SmsCodeRequest req = new SmsCodeRequest();
        req.setPhone(phone);
        req.setScene("login");
        when(valueOps.get("gongguoge:sms:limit:login:" + phone)).thenReturn(null);

        authService.sendSmsCode(req);

        // mock 模式下验证码固定为 123456
        verify(valueOps).set(eq("gongguoge:sms:login:" + phone), eq("123456"), eq(Duration.ofMinutes(5)));
        verify(valueOps).set(eq("gongguoge:sms:limit:login:" + phone), eq("1"), eq(Duration.ofMinutes(1)));
        verify(smsService, times(1)).sendCode(phone, "123456");
        verify(smsLogMapper, times(1)).insert(any());
    }

    // ---------- 启动自检：防止「固定验证码」被带到公网 ----------

    @Test
    @DisplayName("短信关闭且未配置 mock 验证码 -> 启动自检失败（避免任意账号可登录）")
    void startupFailsWhenSmsDisabledWithoutMockCode() {
        AuthService svc = new AuthService(userMapper, smsLogMapper, redisTemplate, jwtUtil, smsService);
        ReflectionTestUtils.setField(svc, "smsEnabled", false);
        ReflectionTestUtils.setField(svc, "mockSmsCode", "");

        IllegalStateException ex = assertThrows(IllegalStateException.class, svc::validateSmsConfig);
        assertTrue(ex.getMessage().contains("mock"), "异常信息应说明缺少 mock 验证码配置");
    }

    @Test
    @DisplayName("短信关闭但配置了 mock 验证码 -> 自检通过（本地开发）")
    void startupOkWhenMockCodeConfigured() {
        assertDoesNotThrow(() -> authService.validateSmsConfig());
    }

    @Test
    @DisplayName("短信开启时无需 mock 验证码 -> 自检通过（生产环境）")
    void startupOkWhenSmsEnabled() {
        AuthService svc = new AuthService(userMapper, smsLogMapper, redisTemplate, jwtUtil, smsService);
        ReflectionTestUtils.setField(svc, "smsEnabled", true);
        ReflectionTestUtils.setField(svc, "mockSmsCode", "");

        assertDoesNotThrow(svc::validateSmsConfig);
    }

    @Test
    @DisplayName("短信开启时验证码为随机值，不再等于固定 mock 值")
    void realModeGeneratesRandomCode() {
        AuthService svc = new AuthService(userMapper, smsLogMapper, redisTemplate, jwtUtil, smsService);
        ReflectionTestUtils.setField(svc, "smsEnabled", true);
        ReflectionTestUtils.setField(svc, "mockSmsCode", "123456");

        SmsCodeRequest req = new SmsCodeRequest();
        req.setPhone("13800000011");
        req.setScene("login");
        when(valueOps.get("gongguoge:sms:limit:login:13800000011")).thenReturn(null);

        // 连续多次发送，验证码应至少出现一次非 123456（随机生成）
        boolean sawNonMock = false;
        for (int i = 0; i < 5 && !sawNonMock; i++) {
            reset(valueOps);
            when(valueOps.get("gongguoge:sms:limit:login:13800000011")).thenReturn(null);
            svc.sendSmsCode(req);
            var captor = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(smsService, atLeastOnce()).sendCode(eq("13800000011"), captor.capture());
            if (!"123456".equals(captor.getAllValues().get(captor.getAllValues().size() - 1))) {
                sawNonMock = true;
            }
        }
        assertTrue(sawNonMock, "短信启用时应使用随机验证码");
    }

    // ---------- 防暴力枚举：校验失败次数限制 ----------

    @Test
    @DisplayName("连续输错达到阈值 -> 验证码作废，必须重新获取")
    void tooManyWrongAttemptsInvalidatesCode() {
        String phone = "13800000012";
        when(valueOps.get("gongguoge:sms:login:" + phone)).thenReturn("123456");
        when(valueOps.increment("gongguoge:sms:fail:login:" + phone)).thenReturn(5L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest(phone, "000000")));
        assertTrue(ex.getMessage().contains("次数过多"), "应提示错误次数过多");
        // 验证码被作废
        verify(redisTemplate).delete("gongguoge:sms:login:" + phone);
    }

    @Test
    @DisplayName("输错但未达阈值 -> 提示验证码错误，验证码保留（用户仍可输入正确码）")
    void wrongAttemptBelowThresholdKeepsCode() {
        String phone = "13800000013";
        when(valueOps.get("gongguoge:sms:login:" + phone)).thenReturn("123456");
        when(valueOps.increment("gongguoge:sms:fail:login:" + phone)).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.login(loginRequest(phone, "000000")));
        assertTrue(ex.getMessage().contains("验证码"));
        verify(redisTemplate, never()).delete("gongguoge:sms:login:" + phone);
    }

    @Test
    @DisplayName("校验通过后清掉失败计数与验证码")
    void successClearsFailCounter() {
        String phone = "13800000014";
        givenExistingUser(phone);
        when(valueOps.get("gongguoge:sms:login:" + phone)).thenReturn("123456");

        authService.login(loginRequest(phone, "123456"));

        verify(redisTemplate).delete("gongguoge:sms:login:" + phone);
        verify(redisTemplate).delete("gongguoge:sms:fail:login:" + phone);
    }

    // ---------- IP 级限流：防「用不同手机号批量刷短信」产生真实费用 ----------

    @Test
    @DisplayName("IP 级限流：同一 IP 超过每小时上限被拒，且不调用短信服务")
    void ipRateLimitBlocksBulkSending() {
        SmsCodeRequest req = new SmsCodeRequest();
        req.setPhone("13800000020");
        req.setScene("login");
        when(valueOps.get("gongguoge:sms:limit:login:13800000020")).thenReturn(null);
        // 已用到第 11 次，超过上限 10
        when(valueOps.increment("gongguoge:sms:iplimit:1.2.3.4")).thenReturn(11L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.sendSmsCode(req, "1.2.3.4"));
        assertTrue(ex.getMessage().contains("过于频繁"), "应提示过于频繁");
        verify(smsService, never()).sendCode(anyString(), anyString());
    }

    @Test
    @DisplayName("IP 级限流：首次发送时给计数键设置 1 小时过期")
    void ipRateLimitSetsOneHourExpiry() {
        SmsCodeRequest req = new SmsCodeRequest();
        req.setPhone("13800000021");
        req.setScene("login");
        when(valueOps.get("gongguoge:sms:limit:login:13800000021")).thenReturn(null);
        when(valueOps.increment("gongguoge:sms:iplimit:1.2.3.4")).thenReturn(1L);

        authService.sendSmsCode(req, "1.2.3.4");

        verify(redisTemplate).expire("gongguoge:sms:iplimit:1.2.3.4", Duration.ofHours(1));
        verify(smsService, times(1)).sendCode(eq("13800000021"), anyString());
    }

    @Test
    @DisplayName("手机号限流优先命中：重复点击不消耗 IP 配额")
    void phoneLimitCheckedBeforeIpLimit() {
        SmsCodeRequest req = new SmsCodeRequest();
        req.setPhone("13800000022");
        req.setScene("login");
        when(valueOps.get("gongguoge:sms:limit:login:13800000022")).thenReturn("1");

        assertThrows(BusinessException.class, () -> authService.sendSmsCode(req, "1.2.3.4"));
        // 手机号级已拦下，不应再去碰 IP 计数
        verify(valueOps, never()).increment("gongguoge:sms:iplimit:1.2.3.4");
    }
}
