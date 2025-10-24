package com.pandora.backend.controller;

import com.pandora.backend.dto.LogDTO;
import com.pandora.backend.entity.Log; // 导入实体类 Log
import com.pandora.backend.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/logs") // 建议使用复数形式 "/logs"，这是 RESTful 规范
public class LogController {

    @Autowired
    private LogService logService;

    // 根据时间获取当天所有日志
    @GetMapping("/byDate")
    public ResponseEntity<List<LogDTO>> getLogsByDate(@RequestParam String datetime) {
        // 前端传入 "2025-10-17T09:30:00" 这样的 ISO 字符串
        LocalDateTime dateTime = LocalDateTime.parse(datetime);
        List<LogDTO> logs = logService.getLogsByDate(dateTime);
        return ResponseEntity.ok(logs);
    }

    // 1. 创建日志 (Create)
    // 你的版本返回 LogDTO，RESTful 风格更推荐返回创建好的完整实体和 201 状态码
    @PostMapping
    public ResponseEntity<Log> createLog(@RequestBody LogDTO dto) {
        Log createdLog = logService.createLog(dto);
        // 返回 201 Created 状态码，并在 Body 中包含创建好的资源
        return new ResponseEntity<>(createdLog, HttpStatus.CREATED);
    }

    // 2.1 查询所有日志 (Read All)
    @GetMapping
    public ResponseEntity<List<LogDTO>> getAllLogs() {
        // 注意：这个方法和你的 getLogsByTask/Date 返回类型不同
        // 它返回的是包含所有字段的 Log 实体列表
        List<LogDTO> logs = logService.getAllLogs();
        return ResponseEntity.ok(logs);
    }

    // 2.2 查询单个日志 (Read by ID)
    @GetMapping("/{id}")
    public ResponseEntity<LogDTO> getLogById(@PathVariable Integer id) {
        LogDTO log = logService.getLogById(id);
        return ResponseEntity.ok(log);
    }

    // 3. 更新/编辑日志 (Update)
    @PutMapping("/{id}")
    public ResponseEntity<LogDTO> updateLog(@PathVariable Integer id, @RequestBody LogDTO logDTO) {
        LogDTO updatedLog = logService.updateLog(id, logDTO);
        return ResponseEntity.ok(updatedLog);
    }

    // 4. 删除日志 (Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLog(@PathVariable Integer id) {
        logService.deleteLog(id);
        // 返回 204 No Content，表示成功处理但无返回体
        return ResponseEntity.noContent().build();
    }

    // TODO 考虑是否与getLogById重复
    @GetMapping("/{logId}/detail")
    public ResponseEntity<LogDTO> getDetailLog(@PathVariable Integer logId) {
        LogDTO detailedLog = logService.getDetailLog(logId);
        return ResponseEntity.ok(detailedLog);
    }

    @GetMapping("/week")
    public ResponseEntity<List<LogDTO>> queryLogsInWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        // 若只传入一个日期，则自动补齐该周的开始与结束
        if (endDate == null) {
            LocalDate startOfWeek = startDate.minusDays(startDate.getDayOfWeek().getValue() - 1);
            LocalDate endOfWeek = startOfWeek.plusDays(6);
            return ResponseEntity.ok(
                    logService.queryLogsInWeek(startOfWeek, endOfWeek));
        } else {
            return ResponseEntity.ok(
                    logService.queryLogsInWeek(startDate, endDate));
        }
    }

    @GetMapping("/month")
    public ResponseEntity<List<LogDTO>> queryLogsInMonth(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (endDate == null) {
            LocalDate startOfMonth = startDate.withDayOfMonth(1);
            LocalDate endOfMonth = startDate.withDayOfMonth(startDate.lengthOfMonth());
            return ResponseEntity.ok(
                    logService.queryLogsInMonth(startOfMonth, endOfMonth));
        } else {
            return ResponseEntity.ok(
                    logService.queryLogsInMonth(startDate, endDate));
        }
    }
}