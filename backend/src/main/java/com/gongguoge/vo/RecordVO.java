package com.gongguoge.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RecordVO {
    private Long id;
    private Integer type;
    private Long categoryId;
    private String categoryName;
    private String content;
    private LocalDate recordDate;
    private String clientId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
