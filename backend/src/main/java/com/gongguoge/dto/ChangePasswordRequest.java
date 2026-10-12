package com.gongguoge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    // 原密码：短信注册、从未设置过密码的账号可留空（此时直接设置新密码）
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 32, message = "新密码长度需在6-32位之间")
    private String newPassword;
}
