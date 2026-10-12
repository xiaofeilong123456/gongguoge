package com.gongguoge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @Size(min = 6, max = 32, message = "密码长度需在6-32位之间")
    private String password;

    private String code;

    /**
     * 仅在账号不存在、走「验证码登录即自动注册」时生效。
     * 供测试脚本显式标记自己的账号，便于后续安全清理（见 backend/scripts）。
     */
    private String nickname;
}
