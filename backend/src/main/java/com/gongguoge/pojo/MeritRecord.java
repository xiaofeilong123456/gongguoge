package com.gongguoge.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_merit_record")
public class MeritRecord extends BaseEntity {
    private Long userId;
    private Integer type;
    private Long categoryId;
    private String content;
    private LocalDate recordDate;
    private String clientId;
    private LocalDateTime deletedAt;
}
