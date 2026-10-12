package com.gongguoge.controller;

import com.gongguoge.common.Result;
import com.gongguoge.common.util.UserContext;
import com.gongguoge.dto.RecordRequest;
import com.gongguoge.service.RecordService;
import com.gongguoge.vo.RecordVO;
import com.gongguoge.vo.TodayVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final RecordService recordService;

    @GetMapping("/today")
    public Result<TodayVO> today() {
        return Result.success(recordService.today(UserContext.getUserId()));
    }

    @GetMapping
    public Result<List<RecordVO>> list(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date != null) {
            return Result.success(recordService.listByDate(UserContext.getUserId(), date));
        }
        return Result.success(recordService.listAll(UserContext.getUserId()));
    }

    @PostMapping
    public Result<RecordVO> add(@Valid @RequestBody RecordRequest request) {
        return Result.success("记录成功", recordService.add(request, UserContext.getUserId()));
    }

    @PutMapping("/{id}")
    public Result<RecordVO> update(@PathVariable Long id, @Valid @RequestBody RecordRequest request) {
        return Result.success("修改成功", recordService.update(id, request, UserContext.getUserId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        recordService.delete(id, UserContext.getUserId());
        return Result.success();
    }

    @GetMapping("/recycle")
    public Result<List<RecordVO>> recycle() {
        return Result.success(recordService.recycleList(UserContext.getUserId()));
    }

    @PostMapping("/recycle/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) {
        recordService.restore(id, UserContext.getUserId());
        return Result.success();
    }

    @DeleteMapping("/recycle/{id}")
    public Result<Void> permanentDelete(@PathVariable Long id) {
        recordService.permanentDelete(id, UserContext.getUserId());
        return Result.success();
    }
}
