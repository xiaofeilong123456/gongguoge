package com.gongguoge.pojo;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_category")
public class Category extends BaseEntity {
    private Long userId;
    private Integer type;
    private String name;
    private Integer sort;
    private Integer isSystem;
}
