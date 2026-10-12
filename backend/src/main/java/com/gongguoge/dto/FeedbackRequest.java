package com.gongguoge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedbackRequest {

    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 500, message = "反馈内容最长500个字符")
    private String content;

    @Size(max = 50, message = "联系方式最长50个字符")
    private String contact;
}
