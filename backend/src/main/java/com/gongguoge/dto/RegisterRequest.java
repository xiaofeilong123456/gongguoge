package com.gongguoge.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 注册请求，支持两条通道（二选一，由 Service 判定）：
 *
 * <ul>
 *   <li><b>手机号通道</b>：{@code phone} + {@code code}（短信验证码，账号更可信）</li>
 *   <li><b>用户名通道</b>：{@code username} + {@code password}（无需手机号）</li>
 * </ul>
 *
 * <p>注意：这里刻意不使用 {@code @NotBlank}，因为「手机号」和「用户名」是互斥的。
 * {@code @Pattern} 对 null 值是放行的，所以两条通道能共用同一个 DTO，
 * 由 Service 用 {@code StringUtils.hasText} 判定走哪条，并给出明确报错。</p>
 */
@Data
public class RegisterRequest {

    /** 约束由 Service 判定，不在此处声明必填 */
    private String nickname;

    // ---------- 手机号通道 ----------

    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    private String code;

    // ---------- 用户名通道 ----------

    @Pattern(regexp = "^[A-Za-z0-9_]{3,20}$", message = "用户名需为 3-20 位字母、数字或下划线")
    private String username;

    @Size(min = 6, max = 32, message = "密码长度需在6-32位之间")
    private String password;
}
