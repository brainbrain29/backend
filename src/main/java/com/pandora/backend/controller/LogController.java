package com.pandora.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.pandora.backend.dto.LogDTO;
import com.pandora.backend.service.LogService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/log")
public class LogController {

    @Autowired
    private LogService logService;

    // 创建日志
    @PostMapping
    public LogDTO createLog(@RequestBody LogDTO dto) {
        return logService.createLog(dto);
    }

    // 根据任务ID获取该任务所有日志
    @GetMapping("/byTask")
    public List<LogDTO> getLogsByTask(@RequestParam Integer taskId) {
        return logService.getLogsByTask(taskId);
    }

    // 根据时间获取当天所有日志
    @GetMapping("/byDate")
    public List<LogDTO> getLogsByDate(@RequestParam String datetime) {
        // 前端传入 "2025-10-17T09:30:00" 这样的 ISO 字符串
        LocalDateTime dateTime = LocalDateTime.parse(datetime);
        return logService.getLogsByDate(dateTime);
    }
}
