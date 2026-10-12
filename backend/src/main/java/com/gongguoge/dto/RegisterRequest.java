package com.gongguoge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String code;

    @Size(max = 30, message = "昵称最长30个字符")
    private String nickname;

    @Size(min = 6, max = 32, message = "密码长度需在6-32位之间")
    private String password;
}
