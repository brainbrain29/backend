package com.pandora.backend.service;

import com.pandora.backend.dto.LogDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.Log;
import com.pandora.backend.entity.Task;
import com.pandora.backend.repository.LogRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

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
    @PersistenceContext
    private EntityManager entityManager;

    // 1. 根据 Task 查询日志并转换为 DTO
    public List<LogDTO> getLogsByTask(Integer taskId) {
        List<Log> logs = logRepository.findByTask_TaskId(taskId);

        return logs.stream().map(log -> {
            LogDTO dto = new LogDTO();
            dto.setEmployeeName(log.getEmployee().getEmployeeName());
            dto.setTaskName(log.getTask() != null ? log.getTask().getTitle() : null);
            dto.setCreatedTime(log.getCreatedTime());
            dto.setContent(log.getContent());
            dto.setEmoji(log.getEmoji());
            dto.setAttachment(log.getAttachment());
            dto.setEmployeeLocation(log.getEmployeeLocation());
            dto.setEmployeeId(log.getEmployee().getEmployeeId());
            dto.setTaskId(log.getTask().getTaskId());
            return dto;
        }).collect(Collectors.toList());
    }

    // 根据时间查询当天所有日志
    public List<LogDTO> getLogsByDate(LocalDateTime dateTime) {
        // 获取 dateTime 所在日期的开始和结束时间
        LocalDateTime startOfDay = dateTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = dateTime.toLocalDate().atTime(LocalTime.MAX);

        // 查询当天日志
        List<Log> logs = logRepository.findByCreatedTimeBetween(startOfDay, endOfDay);

        // 转换为 DTO
        return logs.stream().map(log -> {
            LogDTO dto = new LogDTO();
            dto.setEmployeeName(log.getEmployee().getEmployeeName());
            dto.setTaskName(log.getTask() != null ? log.getTask().getTitle() : null);
            dto.setCreatedTime(log.getCreatedTime());
            dto.setContent(log.getContent());
            dto.setEmoji(log.getEmoji());
            dto.setAttachment(log.getAttachment());
            dto.setEmployeeLocation(log.getEmployeeLocation());
            return dto;
        }).collect(Collectors.toList());
    }

    // 创建新日志
    public LogDTO createLog(LogDTO dto) {
        Log log = new Log();

        Task taskRef = entityManager.getReference(Task.class, dto.getTaskId());
        log.setTask(taskRef);
        Employee empRef = entityManager.getReference(Employee.class, dto.getEmployeeId());
        log.setEmployee(empRef);

        log.setCreatedTime(dto.getCreatedTime() != null ? dto.getCreatedTime() : LocalDateTime.now());
        log.setContent(dto.getContent());
        log.setEmoji(dto.getEmoji());
        log.setAttachment(dto.getAttachment());
        log.setEmployeeLocation(dto.getEmployeeLocation());

        // 保存到数据库
        Log savedLog = logRepository.save(log);

        // 转换为 DTO 返回
        LogDTO result = new LogDTO();
        result.setEmployeeId(savedLog.getEmployee().getEmployeeId());
        result.setEmployeeName(savedLog.getEmployee().getEmployeeName());
        result.setTaskId(savedLog.getTask() != null ? savedLog.getTask().getTaskId() : null);
        result.setTaskName(savedLog.getTask() != null ? savedLog.getTask().getTitle() : null);
        result.setCreatedTime(savedLog.getCreatedTime());
        result.setContent(savedLog.getContent());
        result.setEmoji(savedLog.getEmoji());
        result.setAttachment(savedLog.getAttachment());
        result.setEmployeeLocation(savedLog.getEmployeeLocation());

        return result;
    }
}
