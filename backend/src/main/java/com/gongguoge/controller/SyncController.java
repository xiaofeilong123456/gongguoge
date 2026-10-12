package com.gongguoge.controller;

import com.gongguoge.common.Result;
import com.gongguoge.common.util.UserContext;
import com.gongguoge.dto.SyncBatchRequest;
import com.gongguoge.service.RecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final RecordService recordService;

    @PostMapping("/batch")
    public Result<Map<String, Object>> batch(@Valid @RequestBody SyncBatchRequest request) {
        return Result.success("同步完成", recordService.syncBatch(request.getItems(), UserContext.getUserId()));
    }
}
