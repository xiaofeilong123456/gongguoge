package com.gongguoge.vo;

import lombok.Data;

@Data
public class CategoryVO {
    private Long id;
    private Integer type;
    private String name;
    private Integer sort;
    private Boolean system;
    private Long count;
}
