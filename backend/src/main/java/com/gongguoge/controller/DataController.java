package com.gongguoge.controller;

import com.gongguoge.common.Result;
import com.gongguoge.common.util.UserContext;
import com.gongguoge.service.DataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataController {

    private final DataService dataService;

    @GetMapping("/export")
    public Result<Map<String, Object>> export() {
        return Result.success(dataService.export(UserContext.getUserId()));
    }

    @DeleteMapping("/clear")
    public Result<Void> clear() {
        dataService.clearAll(UserContext.getUserId());
        return Result.success("数据已清空", null);
    }
}
