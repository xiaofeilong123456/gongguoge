package com.gongguoge.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gongguoge.mapper.CategoryMapper;
import com.gongguoge.mapper.MeritRecordMapper;
import com.gongguoge.pojo.Category;
import com.gongguoge.pojo.MeritRecord;
import com.gongguoge.vo.StatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticsService {

    private final MeritRecordMapper meritRecordMapper;
    private final CategoryMapper categoryMapper;

    public StatisticsVO statistics(Long userId, Integer range, Integer year, Integer month) {
        StatisticsVO vo = new StatisticsVO();
        vo.setOverview(overview(userId, year, month));
        vo.setTrend(trend(userId, range == null ? 7 : range));
        vo.setCategory(categoryStats(userId, year, month));
        return vo;
    }

    public Map<String, Object> overview(Long userId, Integer year, Integer month) {
        YearMonth ym = year == null || month == null ? YearMonth.now() : YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<MeritRecord> records = meritRecordMapper.selectList(new LambdaQueryWrapper<MeritRecord>()
                .eq(MeritRecord::getUserId, userId)
                .ge(MeritRecord::getRecordDate, start)
                .le(MeritRecord::getRecordDate, end));

        long total = records.size();
        long positive = records.stream().filter(r -> r.getType() == 1).count();
        long reflect = records.stream().filter(r -> r.getType() == 2).count();
        long days = records.stream().map(MeritRecord::getRecordDate).distinct().count();

        Map<String, Object> map = new HashMap<>();
        map.put("year", ym.getYear());
        map.put("month", ym.getMonthValue());
        map.put("total", total);
        map.put("positive", positive);
        map.put("reflect", reflect);
        map.put("recordDays", days);
        return map;
    }

    public List<Map<String, Object>> trend(Long userId, Integer range) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(range - 1L);
        List<MeritRecord> records = meritRecordMapper.selectList(new LambdaQueryWrapper<MeritRecord>()
                .eq(MeritRecord::getUserId, userId)
                .ge(MeritRecord::getRecordDate, start)
                .le(MeritRecord::getRecordDate, end));

        Map<LocalDate, List<MeritRecord>> grouped = records.stream()
                .collect(Collectors.groupingBy(MeritRecord::getRecordDate));

        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < range; i++) {
            LocalDate date = start.plusDays(i);
            List<MeritRecord> dayRecords = grouped.getOrDefault(date, List.of());
            long positive = dayRecords.stream().filter(r -> r.getType() == 1).count();
            long reflect = dayRecords.stream().filter(r -> r.getType() == 2).count();
            Map<String, Object> item = new HashMap<>();
            item.put("date", date.toString());
            item.put("positive", positive);
            item.put("reflect", reflect);
            item.put("total", positive + reflect);
            result.add(item);
        }
        return result;
    }

    public List<Map<String, Object>> categoryStats(Long userId, Integer year, Integer month) {
        YearMonth ym = year == null || month == null ? YearMonth.now() : YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        List<MeritRecord> records = meritRecordMapper.selectList(new LambdaQueryWrapper<MeritRecord>()
                .eq(MeritRecord::getUserId, userId)
                .ge(MeritRecord::getRecordDate, start)
                .le(MeritRecord::getRecordDate, end));

        Map<Long, Long> countMap = records.stream()
                .collect(Collectors.groupingBy(MeritRecord::getCategoryId, Collectors.counting()));

        List<Category> categories = categoryMapper.selectList(null);
        Map<Long, Category> categoryMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        List<Map<String, Object>> result = new ArrayList<>();
        countMap.forEach((categoryId, count) -> {
            Category category = categoryMap.get(categoryId);
            if (category != null) {
                Map<String, Object> item = new HashMap<>();
                item.put("categoryId", categoryId);
                item.put("categoryName", category.getName());
                item.put("type", category.getType());
                item.put("count", count);
                result.add(item);
            }
        });
        return result;
    }
}
