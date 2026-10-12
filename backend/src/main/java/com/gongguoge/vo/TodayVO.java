package com.gongguoge.vo;

import lombok.Data;

import java.util.List;

@Data
public class TodayVO {
    private Long meritCount;
    private Long faultCount;
    private List<RecordVO> list;
}
