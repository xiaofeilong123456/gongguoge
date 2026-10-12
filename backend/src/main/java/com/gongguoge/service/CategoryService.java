package com.gongguoge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gongguoge.common.exception.BusinessException;
import com.gongguoge.dto.CategoryRequest;
import com.gongguoge.mapper.CategoryMapper;
import com.gongguoge.mapper.MeritRecordMapper;
import com.gongguoge.pojo.Category;
import com.gongguoge.pojo.MeritRecord;
import com.gongguoge.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final MeritRecordMapper meritRecordMapper;

    public List<CategoryVO> list(Long userId) {
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .and(w -> w.eq(Category::getUserId, 0L).or().eq(Category::getUserId, userId))
                .orderByAsc(Category::getType)
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));

        List<MeritRecord> records = meritRecordMapper.selectList(new LambdaQueryWrapper<MeritRecord>()
                .eq(MeritRecord::getUserId, userId));
        Map<Long, Long> countMap = records.stream()
                .collect(Collectors.groupingBy(MeritRecord::getCategoryId, Collectors.counting()));

        List<CategoryVO> result = new ArrayList<>();
        for (Category c : categories) {
            CategoryVO vo = new CategoryVO();
            vo.setId(c.getId());
            vo.setType(c.getType());
            vo.setName(c.getName());
            vo.setSort(c.getSort());
            vo.setSystem(c.getIsSystem() != null && c.getIsSystem() == 1);
            vo.setCount(countMap.getOrDefault(c.getId(), 0L));
            result.add(vo);
        }
        return result;
    }

    public CategoryVO add(CategoryRequest request, Long userId) {
        if (request.getType() == null) {
            request.setType(1);
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException("分类名称不能为空");
        }
        Long exists = categoryMapper.selectCount(new LambdaQueryWrapper<Category>()
                .eq(Category::getType, request.getType())
                .eq(Category::getName, request.getName())
                .and(w -> w.eq(Category::getUserId, 0L).or().eq(Category::getUserId, userId)));
        if (exists != null && exists > 0) {
            throw new BusinessException("该分类已存在");
        }

        Category category = new Category();
        category.setUserId(userId);
        category.setType(request.getType());
        category.setName(request.getName());
        category.setSort(0);
        category.setIsSystem(0);
        categoryMapper.insert(category);

        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setType(category.getType());
        vo.setName(category.getName());
        vo.setSort(category.getSort());
        vo.setSystem(false);
        vo.setCount(0L);
        return vo;
    }

    public void delete(Long id, Long userId) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }
        if (category.getIsSystem() != null && category.getIsSystem() == 1) {
            throw new BusinessException("预设分类不能删除");
        }
        if (category.getUserId() != null && !category.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权删除该分类");
        }
        categoryMapper.deleteById(id);
    }
}
