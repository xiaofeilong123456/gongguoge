package com.gongguoge.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user")
public class User extends BaseEntity {
    /** 手机号，验证码通道使用；用户名注册的账号为空 */
    private String phone;
    /** 用户名，密码通道使用；手机号注册的账号为空 */
    private String username;
    private String password;
    private String nickname;
    private String avatar;
    private Integer status;
}
