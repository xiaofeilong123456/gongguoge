package com.gongguoge.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_sms_log")
public class SmsLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String scene;
    private Integer status;
    private String requestId;
    private String errorMessage;
    private LocalDateTime createTime;
}
