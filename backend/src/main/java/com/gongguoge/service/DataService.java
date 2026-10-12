package com.gongguoge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gongguoge.mapper.CategoryMapper;
import com.gongguoge.mapper.FeedbackMapper;
import com.gongguoge.mapper.MeritRecordMapper;
import com.gongguoge.mapper.UserMapper;
import com.gongguoge.pojo.Category;
import com.gongguoge.pojo.Feedback;
import com.gongguoge.pojo.MeritRecord;
import com.gongguoge.pojo.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DataService {

    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final MeritRecordMapper meritRecordMapper;
    private final FeedbackMapper feedbackMapper;

    public Map<String, Object> export(Long userId) {
        User user = userMapper.selectById(userId);
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .and(w -> w.eq(Category::getUserId, 0L).or().eq(Category::getUserId, userId)));
        List<MeritRecord> records = meritRecordMapper.selectList(new LambdaQueryWrapper<MeritRecord>()
                .eq(MeritRecord::getUserId, userId)
                .orderByDesc(MeritRecord::getRecordDate));
        List<MeritRecord> recycle = meritRecordMapper.selectRecycleByUser(userId);
        List<Feedback> feedback = feedbackMapper.selectList(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getUserId, userId));

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("phone", user.getPhone());
        profile.put("nickname", user.getNickname());
        profile.put("avatar", user.getAvatar());

        Map<String, Object> data = new HashMap<>();
        data.put("profile", profile);
        data.put("categories", categories);
        data.put("records", records);
        data.put("recycle", recycle);
        data.put("feedback", feedback);
        return data;
    }

    @Transactional
    public void clearAll(Long userId) {
        meritRecordMapper.physicalDeleteByUser(userId);
        feedbackMapper.delete(new LambdaQueryWrapper<Feedback>().eq(Feedback::getUserId, userId));
        categoryMapper.delete(new LambdaQueryWrapper<Category>()
                .eq(Category::getUserId, userId)
                .eq(Category::getIsSystem, 0));
    }
}
