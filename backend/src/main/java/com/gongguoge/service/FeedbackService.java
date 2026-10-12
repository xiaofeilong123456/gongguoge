package com.gongguoge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gongguoge.dto.FeedbackRequest;
import com.gongguoge.mapper.FeedbackMapper;
import com.gongguoge.pojo.Feedback;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackMapper feedbackMapper;

    public void submit(FeedbackRequest request, Long userId) {
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setContent(request.getContent());
        feedback.setContact(request.getContact());
        feedback.setStatus(0);
        feedback.setCreateTime(java.time.LocalDateTime.now());
        feedbackMapper.insert(feedback);
    }

    public List<Feedback> list(Long userId) {
        return feedbackMapper.selectList(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getUserId, userId)
                .orderByDesc(Feedback::getCreateTime));
    }
}
