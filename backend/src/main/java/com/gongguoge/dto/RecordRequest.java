package com.gongguoge.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RecordRequest {

    @NotNull(message = "类型不能为空")
    @Min(value = 1, message = "类型只能是1或2")
    @Max(value = 2, message = "类型只能是1或2")
    private Integer type;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    @NotBlank(message = "记录内容不能为空")
    @Size(max = 200, message = "记录内容最长200个字符")
    private String content;

    private LocalDate recordDate;

    private String clientId;
}
