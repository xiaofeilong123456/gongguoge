package com.gongguoge.controller;

import com.gongguoge.common.Result;
import com.gongguoge.common.util.UserContext;
import com.gongguoge.dto.FeedbackRequest;
import com.gongguoge.pojo.Feedback;
import com.gongguoge.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public Result<Void> submit(@Valid @RequestBody FeedbackRequest request) {
        feedbackService.submit(request, UserContext.getUserId());
        return Result.success("反馈已提交，感谢你的建议", null);
    }

    @GetMapping
    public Result<List<Feedback>> list() {
        return Result.success(feedbackService.list(UserContext.getUserId()));
    }
}
