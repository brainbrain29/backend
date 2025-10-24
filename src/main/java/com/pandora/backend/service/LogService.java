package com.pandora.backend.service;

import com.pandora.backend.dto.LogDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.Log;
import com.pandora.backend.entity.Task;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.repository.LogRepository;
import com.pandora.backend.repository.TaskRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LogService {

    @Autowired
    private LogRepository logRepository;
    @Autowired
    private EmployeeRepository employeeRepository; // 注入 EmployeeRepository
    @Autowired
    private TaskRepository taskRepository; // 注入 TaskRepository

    // ==================================================
    // 你的已有方法 (完全保留)
    // ==================================================

    // TODO
    // 根据 Task 查询日志并转换为 DTO
    public List<LogDTO> getLogsByTask(Integer taskId) {
        List<Log> logs = logRepository.findByTask_TaskId(taskId);
        return convertToDtoList(logs); // 使用重构的转换方法
    }

    // TODO
    // 根据时间查询当天所有日志
    public List<LogDTO> getLogsByDate(LocalDateTime dateTime) {
        LocalDateTime startOfDay = dateTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = dateTime.toLocalDate().atTime(LocalTime.MAX);
        List<Log> logs = logRepository.findByCreatedTimeBetween(startOfDay, endOfDay);
        return convertToDtoList(logs); // 使用重构的转换方法
    }

    // ==================================================
    // 新增的标准 CRUD 方法 (合并进来)
    // ==================================================

    // 1. 创建日志 (Create)
    // 返回实体 Log，与 Controller 的 ResponseEntity<Log> 对应
    public Log createLog(LogDTO dto) {
        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + dto.getEmployeeId()));

        Log log = new Log();
        log.setEmployee(employee);
        log.setContent(dto.getContent());
        log.setEmoji(dto.getEmoji());
        log.setAttachment(dto.getAttachment());
        log.setEmployeeLocation(dto.getEmployeeLocation());
        log.setCreatedTime(LocalDateTime.now()); // 由后端设置创建时间

        if (dto.getTaskId() != null) {
            Task task = taskRepository.findById(dto.getTaskId())
                    .orElseThrow(() -> new RuntimeException("Task not found with id: " + dto.getTaskId()));
            log.setTask(task);
        }

        return logRepository.save(log);
    }

    // TODO
    // 2.1 查询所有日志 (Read All)
    // 直接返回实体列表，因为某些场景可能需要完整的实体信息
    public List<LogDTO> getAllLogs() {
        return logRepository.findAll().stream()
                .map(log -> {
                    LogDTO dto = new LogDTO();
                    dto.setLogId(log.getLogId());

                    // 员工信息
                    if (log.getEmployee() != null) {
                        dto.setEmployeeId(log.getEmployee().getEmployeeId());
                        dto.setEmployeeName(log.getEmployee().getEmployeeName());
                    }

                    // 任务信息
                    if (log.getTask() != null) {
                        dto.setTaskId(log.getTask().getTaskId());
                        dto.setTaskName(log.getTask().getTitle()); // 假设 title 作为 taskName
                    }

                    dto.setCreatedTime(log.getCreatedTime());
                    dto.setContent(log.getContent());
                    dto.setEmoji(log.getEmoji());
                    dto.setAttachment(log.getAttachment());
                    dto.setEmployeeLocation(log.getEmployeeLocation());

                    return dto;
                })
                .collect(Collectors.toList());
    }

    // TODO
    // 2.2 查询单个日志 (Read by ID)
    public LogDTO getLogById(Integer logId) {
        Log log = logRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log not found with id: " + logId));

        LogDTO dto = new LogDTO();
        dto.setLogId(log.getLogId());
        dto.setEmployeeId(log.getEmployee() != null ? log.getEmployee().getEmployeeId() : null);
        dto.setEmployeeName(log.getEmployee() != null ? log.getEmployee().getEmployeeName() : null);
        dto.setTaskId(log.getTask() != null ? log.getTask().getTaskId() : null);
        dto.setTaskName(log.getTask() != null ? log.getTask().getTitle() : null);
        dto.setCreatedTime(log.getCreatedTime());
        dto.setContent(log.getContent());
        dto.setEmoji(log.getEmoji());
        dto.setAttachment(log.getAttachment());
        dto.setEmployeeLocation(log.getEmployeeLocation());

        return dto;
    }

    // 3. 更新/编辑日志 (Update)
    public LogDTO updateLog(Integer id, LogDTO logDTO) {
        Log existingLog = logRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log not found with id: " + id));

        // 更新字段
        existingLog.setContent(logDTO.getContent());
        existingLog.setEmoji(logDTO.getEmoji());
        existingLog.setAttachment(logDTO.getAttachment());
        existingLog.setEmployeeLocation(logDTO.getEmployeeLocation());

        // 更新关联 Task
        if (logDTO.getTaskId() != null) {
            Task task = taskRepository.findById(logDTO.getTaskId())
                    .orElseThrow(() -> new RuntimeException("Task not found with id: " + logDTO.getTaskId()));
            existingLog.setTask(task);
        } else {
            existingLog.setTask(null);
        }

        // 保存更新
        Log savedLog = logRepository.save(existingLog);

        // 构造 LogDTO 返回
        LogDTO resultDTO = new LogDTO();
        resultDTO.setLogId(savedLog.getLogId());
        resultDTO.setEmployeeId(savedLog.getEmployee().getEmployeeId());
        resultDTO.setEmployeeName(savedLog.getEmployee().getEmployeeName());
        resultDTO.setTaskId(savedLog.getTask() != null ? savedLog.getTask().getTaskId() : null);
        resultDTO.setTaskName(savedLog.getTask() != null ? savedLog.getTask().getTitle() : null);
        resultDTO.setContent(savedLog.getContent());
        resultDTO.setEmoji(savedLog.getEmoji());
        resultDTO.setAttachment(savedLog.getAttachment());
        resultDTO.setEmployeeLocation(savedLog.getEmployeeLocation());
        resultDTO.setCreatedTime(savedLog.getCreatedTime());

        return resultDTO;
    }

    // 4. 删除日志 (Delete)
    public void deleteLog(Integer id) {
        if (!logRepository.existsById(id)) {
            throw new RuntimeException("Log not found with id: " + id);
        }
        logRepository.deleteById(id);
    }

    public List<LogDTO> queryLogsInWeek(LocalDate startDate, LocalDate endDate) {
        if (endDate == null) {
            endDate = startDate.plusDays(6); // 默认一周
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<Log> logs = logRepository.findByCreatedTimeBetween(startDateTime, endDateTime);
        return logs.stream().map(this::convertToDto).toList();
    }

    public List<LogDTO> queryLogsInMonth(LocalDate startDate, LocalDate endDate) {
        if (endDate == null) {
            endDate = startDate.withDayOfMonth(startDate.lengthOfMonth()); // 当月最后一天
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<Log> logs = logRepository.findByCreatedTimeBetween(startDateTime, endDateTime);
        return logs.stream().map(this::convertToDto).toList();
    }

    public LogDTO getDetailLog(Integer logId) {
        Log log = logRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log not found with id: " + logId));
        return convertToDto(log);
    }

    // ==================================================
    // 辅助方法 (重构优化)
    // ==================================================

    // 将 Log 列表转换为 LogDTO 列表的通用方法，避免代码重复
    private List<LogDTO> convertToDtoList(List<Log> logs) {
        return logs.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // TODO 使用批处理减少employeeId,name等查询
    // 将单个 Log 实体转换为 LogDTO 的通用方法
    private LogDTO convertToDto(Log log) {
        LogDTO dto = new LogDTO();
        dto.setLogId(log.getLogId());
        dto.setEmployeeName(log.getEmployee().getEmployeeName());
        dto.setEmployeeId(log.getEmployee().getEmployeeId());
        dto.setTaskName(log.getTask() != null ? log.getTask().getTitle() : null);
        dto.setTaskId(log.getTask() != null ? log.getTask().getTaskId() : null);
        dto.setCreatedTime(log.getCreatedTime());
        dto.setContent(log.getContent());
        dto.setEmoji(log.getEmoji());
        dto.setAttachment(log.getAttachment());
        dto.setEmployeeLocation(log.getEmployeeLocation());
        return dto;
    }
}