package com.gongguoge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequest {

    @Min(value = 1, message = "类型只能是1或2")
    @Max(value = 2, message = "类型只能是1或2")
    private Integer type;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 30, message = "分类名称最长30个字符")
    private String name;
}
