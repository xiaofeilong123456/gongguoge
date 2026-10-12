package com.gongguoge.controller;

import com.gongguoge.common.Result;
import com.gongguoge.common.util.UserContext;
import com.gongguoge.service.StatisticsService;
import com.gongguoge.vo.StatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping
    public Result<StatisticsVO> statistics(
            @RequestParam(required = false) Integer range,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return Result.success(statisticsService.statistics(UserContext.getUserId(), range, year, month));
    }

    @GetMapping("/overview")
    public Result<java.util.Map<String, Object>> overview(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return Result.success(statisticsService.overview(UserContext.getUserId(), year, month));
    }

    @GetMapping("/trend")
    public Result<java.util.List<java.util.Map<String, Object>>> trend(@RequestParam(defaultValue = "7") Integer range) {
        return Result.success(statisticsService.trend(UserContext.getUserId(), range));
    }

    @GetMapping("/category")
    public Result<java.util.List<java.util.Map<String, Object>>> category(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return Result.success(statisticsService.categoryStats(UserContext.getUserId(), year, month));
    }
}
