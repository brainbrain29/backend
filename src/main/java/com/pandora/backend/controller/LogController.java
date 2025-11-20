package com.pandora.backend.controller;

import com.pandora.backend.dto.LogDTO;
import com.pandora.backend.dto.TaskDTO;
import com.pandora.backend.entity.Log; // 导入实体类 Log
import com.pandora.backend.entity.LogAttachment;
import com.pandora.backend.repository.LogAttachmentRepository;
import com.pandora.backend.service.LogService;
import com.pandora.backend.service.AttachmentService;
import com.pandora.backend.service.FileStorageService;
import com.pandora.backend.service.TaskService;
import com.pandora.backend.util.JwtUtil;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.pandora.backend.service.TaskService;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/logs") // 建议使用复数形式 "/logs"，这是 RESTful 规范
public class LogController {

    @Autowired
    private LogService logService;

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private JwtUtil jwtUtil;

    // 根据时间获取当天所有日志
    @GetMapping("/byDate")
    public ResponseEntity<?> getLogsByDate(HttpServletRequest request, @RequestParam LocalDate datetime) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;
        // 前端传入 "2025-10-17" 这样的 ISO 字符串
        LocalDateTime dateTime = datetime.atStartOfDay();
        List<LogDTO> logs = logService.getLogsByDate(userId, dateTime);
        return ResponseEntity.ok(logs);
    }

    // 1. 创建日志 (Create)
    // 支持文件上传,使用 multipart/form-data
    @PostMapping
    public ResponseEntity<?> createLog(
            HttpServletRequest request,
            @RequestParam("content") String content,
            @RequestParam(value = "mood", required = false) String mood,
            @RequestParam(value = "taskId", required = false) Integer taskId,
            @RequestParam(value = "employeeLocation", required = false) String employeeLocation,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {

        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;

        try {
            // 构建 DTO
            LogDTO dto = new LogDTO();
            dto.setEmployeeId(userId);
            dto.setContent(content);
            // 直接设置mood字符串，后续在service中会转换为Emoji枚举
            dto.setEmoji(mood);
            dto.setTaskId(taskId);
            dto.setEmployeeLocation(employeeLocation); // 设置员工位置

            // 创建日志并处理附件
            Log createdLog = logService.createLogWithAttachments(dto, files, userId);
            LogDTO createdDto = logService.getLogById(createdLog.getLogId());

            return new ResponseEntity<>(createdDto, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("创建日志失败: " + e.getMessage());
        }
    }

    // 2.1 查询所有日志 (Read All)
    @GetMapping
    public ResponseEntity<List<LogDTO>> getAllLogs() {
        // 注意：这个方法和你的 getLogsByTask/Date 返回类型不同
        // 它返回的是包含所有字段的 Log 实体列表
        List<LogDTO> logs = logService.getAllLogs();
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/search")
    public ResponseEntity<List<LogDTO>> searchLogs(
            HttpServletRequest request,
            @RequestParam(value = "keyword", required = false) String keyword) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        Integer userId = (Integer) uidObj;

        List<LogDTO> logs = logService.searchLogs(keyword, userId);
        return ResponseEntity.ok(logs);
    }

    // 2.2 查询单个日志 (Read by ID)
    @GetMapping("/{id}")
    public ResponseEntity<?> getLogById(HttpServletRequest request, @PathVariable Integer id) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        LogDTO log = logService.getLogById((Integer) id);
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

    // TODO 检查数据库查询效率和数据库时间
    @GetMapping("/week")
    public ResponseEntity<?> queryLogsInWeek(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        // 若只传入一个日期，则自动补齐该周的开始与结束
        if (endDate == null) {
            LocalDate startOfWeek = startDate.minusDays(startDate.getDayOfWeek().getValue() - 1);
            LocalDate endOfWeek = startOfWeek.plusDays(6);
            return ResponseEntity.ok(
                    logService.queryLogsInWeek((Integer) uidObj, startOfWeek, endOfWeek));
        } else {
            return ResponseEntity.ok(
                    logService.queryLogsInWeek((Integer) uidObj, startDate, endDate));
        }
    }

    // TODO 检查数据库查询效率和数据库时间
    @GetMapping("/month")
    public ResponseEntity<?> queryLogsInMonth(
            HttpServletRequest request,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;
        if (endDate == null) {
            LocalDate startOfMonth = startDate.withDayOfMonth(1);
            LocalDate endOfMonth = startDate.withDayOfMonth(startDate.lengthOfMonth());
            return ResponseEntity.ok(
                    logService.queryLogsInMonth(userId, startOfMonth, endOfMonth));
        } else {
            return ResponseEntity.ok(
                    logService.queryLogsInMonth(userId, startDate, endDate));
        }
    }

    // 当前登录用户负责的未完成任务列表,用于日志关联
    @GetMapping("/tasks")
    public ResponseEntity<?> getUnfinishedTasksForLog(HttpServletRequest request) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;

        List<TaskDTO> tasks = taskService.getUnfinishedTasksForLog(userId);
        return ResponseEntity.ok(tasks);
    }

    /**
     * API 接口：下载日志附件
     * 使用通用的 AttachmentService
     */
    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable("id") Long attachmentId,
            @RequestParam(value = "token", required = false) String tokenParam,
            HttpServletRequest request) {
        return attachmentService.downloadLogAttachment(attachmentId, tokenParam, request);
    }

    /**
     * API 接口：预览日志附件
     * 使用通用的 AttachmentService
     */
    @GetMapping("/attachments/{id}/preview")
    public ResponseEntity<Resource> previewAttachment(
            @PathVariable("id") Long attachmentId,
            @RequestParam(value = "token", required = false) String tokenParam,
            HttpServletRequest request) {
        return attachmentService.previewLogAttachment(attachmentId, tokenParam, request);
    }
}