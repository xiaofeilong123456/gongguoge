package com.gongguoge.vo;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    /** 手机号通道账号；用户名注册的账号为 null */
    private String phone;
    /** 用户名通道账号；手机号注册的账号为 null */
    private String username;
    private String nickname;
    private String avatar;
}
