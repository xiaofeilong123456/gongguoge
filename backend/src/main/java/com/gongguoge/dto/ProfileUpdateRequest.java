package com.gongguoge.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {

    @Size(max = 30, message = "昵称最长30个字符")
    private String nickname;

    private String avatar;
}
