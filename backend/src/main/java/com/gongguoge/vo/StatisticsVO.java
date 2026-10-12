package com.gongguoge.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class StatisticsVO {
    private Map<String, Object> overview;
    private List<Map<String, Object>> trend;
    private List<Map<String, Object>> category;
}
