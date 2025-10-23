package com.pandora.backend.service;

import com.pandora.backend.dto.LogDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.Log;
import com.pandora.backend.entity.Task;
import com.pandora.backend.repository.EmployeeRepository; // 导入 EmployeeRepository
import com.pandora.backend.repository.LogRepository;
import com.pandora.backend.repository.TaskRepository; // 导入 TaskRepository
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
    private TaskRepository TaskRepository; // 注入 TaskRepository

    //==================================================
    // 你的已有方法 (完全保留)
    //==================================================

    // 根据 Task 查询日志并转换为 DTO
    public List<LogDTO> getLogsByTask(Integer taskId) {
        List<Log> logs = logRepository.findByTask_TaskId(taskId);
        return convertToDtoList(logs); // 使用重构的转换方法
    }

    // 根据时间查询当天所有日志
    public List<LogDTO> getLogsByDate(LocalDateTime dateTime) {
        LocalDateTime startOfDay = dateTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = dateTime.toLocalDate().atTime(LocalTime.MAX);
        List<Log> logs = logRepository.findByCreatedTimeBetween(startOfDay, endOfDay);
        return convertToDtoList(logs); // 使用重构的转换方法
    }


    //==================================================
    // 新增的标准 CRUD 方法 (合并进来)
    //==================================================

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
            Task task = TaskRepository.findById(dto.getTaskId())
                    .orElseThrow(() -> new RuntimeException("Task not found with id: " + dto.getTaskId()));
            log.setTask(task);
        }

        return logRepository.save(log);
    }

    // 2.1 查询所有日志 (Read All)
    // 直接返回实体列表，因为某些场景可能需要完整的实体信息
    public List<Log> getAllLogs() {
        return logRepository.findAll();
    }

    // 2.2 查询单个日志 (Read by ID)
    public Log getLogById(Integer id) {
        return logRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log not found with id: " + id));
    }

    // 3. 更新/编辑日志 (Update)
    public Log updateLog(Integer id, LogDTO logDTO) {
        Log existingLog = getLogById(id);

        existingLog.setContent(logDTO.getContent());
        existingLog.setEmoji(logDTO.getEmoji());
        existingLog.setAttachment(logDTO.getAttachment());
        existingLog.setEmployeeLocation(logDTO.getEmployeeLocation());

        // 根据业务需求决定是否允许更新关联关系
        if (logDTO.getTaskId() != null) {
            Task task = TaskRepository.findById(logDTO.getTaskId())
                    .orElseThrow(() -> new RuntimeException("Task not found with id: " + logDTO.getTaskId()));
            existingLog.setTask(task);
        } else {
            existingLog.setTask(null);
        }

        return logRepository.save(existingLog);
    }

    // 4. 删除日志 (Delete)
    public void deleteLog(Integer id) {
        if (!logRepository.existsById(id)) {
            throw new RuntimeException("Log not found with id: " + id);
        }
        logRepository.deleteById(id);
    }

    //==================================================
    // 辅助方法 (重构优化)
    //==================================================

    // 将 Log 列表转换为 LogDTO 列表的通用方法，避免代码重复
    private List<LogDTO> convertToDtoList(List<Log> logs) {
        return logs.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // 将单个 Log 实体转换为 LogDTO 的通用方法
    private LogDTO convertToDto(Log log) {
        LogDTO dto = new LogDTO();
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