package com.gongguoge.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 登录请求，支持三种方式（由 Service 判定优先级）：
 *
 * <ol>
 *   <li><b>用户名 + 密码</b>：{@code username} + {@code password}</li>
 *   <li><b>手机号 + 密码</b>：{@code phone} + {@code password}</li>
 *   <li><b>手机号 + 验证码</b>：{@code phone} + {@code code}（未注册会自动注册）</li>
 * </ol>
 *
 * <p>注意：这里刻意不使用 {@code @NotBlank} —— 手机号与用户名是互斥的，
 * {@code @Pattern} 对 null 放行，所以三种方式能共用同一个 DTO，
 * 由 Service 用 {@code StringUtils.hasText} 判定并给出明确报错。</p>
 */
@Data
public class LoginRequest {

    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @Pattern(regexp = "^[A-Za-z0-9_]{3,20}$", message = "用户名格式不正确")
    private String username;

    @Size(min = 6, max = 32, message = "密码长度需在6-32位之间")
    private String password;

    private String code;

    /**
     * 仅在账号不存在、走「验证码登录即自动注册」时生效。
     * 供测试脚本显式标记自己的账号，便于后续安全清理（见 backend/scripts）。
     */
    private String nickname;
}
