package com.gongguoge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gongguoge.common.exception.BusinessException;
import com.gongguoge.common.util.JwtUtil;
import com.gongguoge.dto.ChangePasswordRequest;
import com.gongguoge.dto.LoginRequest;
import com.gongguoge.dto.RegisterRequest;
import com.gongguoge.dto.SmsCodeRequest;
import com.gongguoge.mapper.SmsLogMapper;
import com.gongguoge.mapper.UserMapper;
import com.gongguoge.pojo.SmsLog;
import com.gongguoge.pojo.User;
import com.gongguoge.vo.LoginVO;
import com.gongguoge.vo.UserVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String SMS_CODE_PREFIX = "gongguoge:sms:";
    private static final String SMS_LIMIT_PREFIX = "gongguoge:sms:limit:";
    private static final String SMS_FAIL_PREFIX = "gongguoge:sms:fail:";
    private static final String SMS_IP_LIMIT_PREFIX = "gongguoge:sms:iplimit:";
    /** 同一客户端 IP 每小时最多发几次验证码（防批量刷短信产生费用） */
    private static final int SMS_IP_LIMIT_PER_HOUR = 10;
    /** 同一验证码允许的最大连续失败次数，超过即作废（防暴力枚举） */
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    /** 验证码用安全随机数：java.util.Random 是可预测的，不适合安全场景 */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserMapper userMapper;
    private final SmsLogMapper smsLogMapper;
    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;
    private final SmsService smsService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${aliyun.sms.enabled:true}")
    private boolean smsEnabled;

    /**
     * 短信未启用时使用的固定验证码，仅供本地开发。
     *
     * <p>故意不给默认值：缺失时启动直接失败，避免"任意手机号 + 固定验证码即可登录"
     * 这套开发配置被原样带到公网。</p>
     */
    @Value("${aliyun.sms.mock-code:}")
    private String mockSmsCode;

    /**
     * 启动自检：短信关闭时必须显式配置 mock 验证码。
     *
     * <p>若不校验，短信关闭 + 固定验证码的默认组合会造成：任何手机号都能用同一个验证码登录，
     * 而登录接口对未知手机号还会自动注册——部署到公网等于所有账号可被接管。</p>
     */
    @PostConstruct
    void validateSmsConfig() {
        if (!smsEnabled && !StringUtils.hasText(mockSmsCode)) {
            throw new IllegalStateException(
                    "短信服务未启用（aliyun.sms.enabled=false）且未配置 mock 验证码。"
                            + "生产环境请设置 ALIYUN_SMS_ENABLED=true 并填入真实密钥；"
                            + "开发环境请在 application-local.yml 配置 aliyun.sms.mock-code（例如 123456），"
                            + "或设置环境变量 ALIYUN_SMS_MOCK_CODE。"
                            + "此校验用于避免误用固定验证码上线导致任意账号可被登录。");
        }
    }

    /** 兼容旧调用（不带客户端 IP，不做 IP 级限流） */
    public void sendSmsCode(SmsCodeRequest request) {
        sendSmsCode(request, null);
    }

    /**
     * 发送验证码。
     *
     * <p>两层限流：
     * <ol>
     *   <li><b>按手机号</b>：同一号码 1 分钟只能发一次（防用户手抖/短信轰炸单号码）</li>
     *   <li><b>按客户端 IP</b>：同一 IP 每小时最多 {@value #SMS_IP_LIMIT_PER_HOUR} 次。
     *       这一层是必须的——否则别人拿到接口地址后可以用**不同的手机号**批量刷短信，
     *       每条都产生真实费用。</li>
     * </ol>
     */
    public void sendSmsCode(SmsCodeRequest request, String clientIp) {
        String phone = request.getPhone();
        String scene = request.getScene();
        String limitKey = SMS_LIMIT_PREFIX + scene + ":" + phone;

        // ① 手机号级限流：先判，避免用户重复点击时消耗 IP 配额
        String limit = redisTemplate.opsForValue().get(limitKey);
        if (limit != null && Integer.parseInt(limit) >= 1) {
            throw new BusinessException("验证码发送过于频繁，请1分钟后再试");
        }

        // ② IP 级限流：防止用不同手机号批量刷短信（会产生真实费用）
        if (StringUtils.hasText(clientIp)) {
            String ipKey = SMS_IP_LIMIT_PREFIX + clientIp;
            Long used = redisTemplate.opsForValue().increment(ipKey);
            if (used != null) {
                if (used == 1L) {
                    redisTemplate.expire(ipKey, Duration.ofHours(1));
                }
                if (used > SMS_IP_LIMIT_PER_HOUR) {
                    throw new BusinessException("当前网络发送验证码过于频繁，请稍后再试");
                }
            }
        }

        String code = smsEnabled ? String.format("%06d", SECURE_RANDOM.nextInt(1000000)) : mockSmsCode;
        smsService.sendCode(phone, code);
        redisTemplate.opsForValue().set(SMS_CODE_PREFIX + scene + ":" + phone, code, Duration.ofMinutes(5));
        redisTemplate.opsForValue().set(limitKey, "1", Duration.ofMinutes(1));

        SmsLog smsLog = new SmsLog();
        smsLog.setPhone(phone);
        smsLog.setScene(scene);
        smsLog.setStatus(1);
        smsLog.setRequestId(UUID.randomUUID().toString().replace("-", ""));
        smsLog.setCreateTime(LocalDateTime.now());
        smsLogMapper.insert(smsLog);

    }

    /**
     * 注册。两条通道二选一：
     * <ul>
     *   <li><b>手机号通道</b>：phone + code（需短信验证码，账号更可信）</li>
     *   <li><b>用户名通道</b>：username + password（无需手机号）</li>
     * </ul>
     */
    public LoginVO register(RegisterRequest request) {
        if (StringUtils.hasText(request.getUsername())) {
            return registerByUsername(request);
        }
        if (!StringUtils.hasText(request.getPhone())) {
            throw new BusinessException(400, "请填写手机号与验证码，或改用用户名注册");
        }
        if (!StringUtils.hasText(request.getCode())) {
            throw new BusinessException(400, "验证码不能为空");
        }

        String phone = request.getPhone();
        verifySmsCode(phone, "register", request.getCode());

        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
        if (count != null && count > 0) {
            throw new BusinessException(1001, "账号已存在");
        }

        User user = new User();
        user.setPhone(phone);
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : "功过格用户");
        String rawPassword = StringUtils.hasText(request.getPassword()) ? request.getPassword() : UUID.randomUUID().toString().substring(0, 8);
        checkPasswordStrength(rawPassword);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setStatus(1);
        userMapper.insert(user);

        return buildLoginVO(user);
    }

    /**
     * 用户名通道注册。
     *
     * <p>之所以单独开一条「用户名」通道，而不是允许「手机号 + 密码免验证码注册」：
     * 后者会让任何人都能拿别人的真实手机号抢先注册（手机号无法被验证），
     * 是个实打实的安全漏洞。用户名与手机号互不干扰，不存在抢占问题。</p>
     */
    private LoginVO registerByUsername(RegisterRequest request) {
        String username = request.getUsername().trim();
        // 11 位纯数字与手机号长得一模一样，登录时无法区分该走哪条通道，直接禁掉
        if (username.matches("^\\d{11}$")) {
            throw new BusinessException(400, "用户名不能是 11 位纯数字（会与手机号混淆）");
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(400, "密码不能为空");
        }
        checkPasswordStrength(request.getPassword());

        // 唯一索引是 utf8mb4_unicode_ci（大小写不敏感），这里的查询与其保持一致，
        // 避免出现 Test / test 两个「看起来一样」的账号
        Long count = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count != null && count > 0) {
            throw new BusinessException(1001, "用户名已被占用");
        }

        User user = new User();
        user.setUsername(username);
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : "功过格用户");
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(1);
        userMapper.insert(user);

        return buildLoginVO(user);
    }

    /** 密码强度下限：长度已由 DTO 约束（6-32），这里只挡掉弱到离谱的纯数字密码。 */
    private void checkPasswordStrength(String password) {
        if (password != null && password.matches("^\\d+$")) {
            throw new BusinessException(400, "密码不能是纯数字");
        }
    }

    /**
     * 登录。按以下优先级判定方式：
     * <ol>
     *   <li><b>用户名 + 密码</b>（与手机号无关的通道）</li>
     *   <li><b>手机号 + 验证码</b>（未注册则自动注册）</li>
     *   <li><b>手机号 + 密码</b></li>
     * </ol>
     */
    public LoginVO login(LoginRequest request) {
        // ① 用户名通道
        if (StringUtils.hasText(request.getUsername())) {
            return loginByUsername(request);
        }

        if (!StringUtils.hasText(request.getPhone())) {
            throw new BusinessException(400, "请输入用户名或手机号");
        }

        // ② 验证码登录：已注册直接登录，未注册自动注册并登录
        if (StringUtils.hasText(request.getCode())) {
            verifySmsCode(request.getPhone(), "login", request.getCode());
            User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
            if (user == null) {
                user = new User();
                user.setPhone(request.getPhone());
                // 允许调用方指定昵称，便于测试脚本标记自己的账号（默认仍为「功过格用户」）
                user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : "功过格用户");
                user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString().substring(0, 8)));
                user.setStatus(1);
                userMapper.insert(user);
            }
            if (user.getStatus() == null || user.getStatus() != 1) {
                throw new BusinessException(403, "账号已被禁用");
            }
            return buildLoginVO(user);
        }

        // ③ 手机号 + 密码登录：必须已注册
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, request.getPhone()));
        if (user == null) {
            throw new BusinessException(1002, "账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }
        if (!StringUtils.hasText(request.getPassword()) || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(1002, "账号或密码错误");
        }
        return buildLoginVO(user);
    }

    /** 用户名 + 密码登录。 */
    private LoginVO loginByUsername(LoginRequest request) {
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(400, "密码不能为空");
        }
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername().trim()));
        if (user == null) {
            throw new BusinessException(1002, "账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }
        if (!StringUtils.hasText(user.getPassword())
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(1002, "账号或密码错误");
        }
        return buildLoginVO(user);
    }

    public void changePassword(ChangePasswordRequest request, Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }
        // 已有密码的账号必须校验原密码；短信注册、从未设置过密码（password 为空）的账号可直接设置
        if (StringUtils.hasText(user.getPassword()) && !passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(user);
    }

    private LoginVO buildLoginVO(User user) {
        LoginVO vo = new LoginVO();
        vo.setToken(jwtUtil.generateToken(user.getId(), user.getPhone()));
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setPhone(user.getPhone());
        userVO.setUsername(user.getUsername());
        userVO.setNickname(user.getNickname());
        userVO.setAvatar(user.getAvatar());
        vo.setUserInfo(userVO);
        return vo;
    }

    /**
     * 校验验证码。
     *
     * <p>除了「一次性消费」（成功即删除，不可重放），还做了**校验失败次数限制**：
     * 6 位验证码只有 100 万种可能，若不限次数，攻击者可在 5 分钟有效期内暴力枚举。
     * 失败达到阈值后直接作废该验证码，必须重新获取。</p>
     */
    private void verifySmsCode(String phone, String scene, String code) {
        String key = SMS_CODE_PREFIX + scene + ":" + phone;
        String failKey = SMS_FAIL_PREFIX + scene + ":" + phone;

        String cached = redisTemplate.opsForValue().get(key);
        if (cached == null) {
            throw new BusinessException("验证码错误或已过期");
        }
        if (!cached.equals(code)) {
            Long fails = redisTemplate.opsForValue().increment(failKey);
            if (fails != null) {
                // 失败计数与验证码同时失效（5 分钟），避免长期锁定
                redisTemplate.expire(failKey, Duration.ofMinutes(5));
                if (fails >= MAX_VERIFY_ATTEMPTS) {
                    // 作废验证码，强制重新获取
                    redisTemplate.delete(key);
                    redisTemplate.delete(failKey);
                    throw new BusinessException("验证码错误次数过多，请重新获取");
                }
            }
            throw new BusinessException("验证码错误或已过期");
        }
        // 校验通过：一次性消费，并清掉失败计数
        redisTemplate.delete(key);
        redisTemplate.delete(failKey);
    }
}
