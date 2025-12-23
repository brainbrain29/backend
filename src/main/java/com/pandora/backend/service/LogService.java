package com.pandora.backend.service;

// 导入 DTO
import com.pandora.backend.dto.AttachmentDTO;
import com.pandora.backend.dto.LogDTO;

// 导入 实体
import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.Log;
import com.pandora.backend.entity.LogAttachment;
import com.pandora.backend.entity.Task;

// 导入 Repositories
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.repository.LogRepository;
import com.pandora.backend.repository.TaskRepository;
import com.pandora.backend.repository.LogAttachmentRepository;

// 导入其他
import com.pandora.backend.enums.Emoji;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set; // 导入 Set
import java.util.stream.Collectors;

@Service
public class LogService {

    @Autowired
    private LogRepository logRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private LogAttachmentRepository logAttachmentRepository;
    // @Autowired
    // private FileStorageService fileStorageService;
    @Autowired
    private OssService ossService;

    // =================================================:
    // 附件创建 (唯一保留的创建方法)
    // =================================================:

    /**
     * 创建一个新日志，并处理附件
     * 
     * @param dto    包含日志文本信息的 DTO
     * @param files  用户上传的文件数组
     * @param userId 当前登录的用户ID
     * @return 保存后的 Log 实体
     */
    @Transactional
    public Log createLogWithAttachments(LogDTO dto, MultipartFile[] files, Integer userId) {

        // 1. 查找关联的实体
        Employee employee = employeeRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("未找到 Employee ID: " + userId));

        Task task = null;
        if (dto.getTaskId() != null) {
            task = taskRepository.findById(dto.getTaskId()).orElse(null);
        }

        // 2. 先保存"主日志"
        Log newLog = new Log();
        newLog.setEmployee(employee);
        newLog.setTask(task);
        newLog.setContent(dto.getContent());
        newLog.setEmployeeLocation(dto.getEmployeeLocation()); // 设置员工位置

        // DTO 中的 emoji 传的是 Emoji 的中文描述，例如 "平静"、"开心"
        if (dto.getEmoji() != null) {
            // 将中文描述转换为 Emoji 枚举
            Emoji emoji = Emoji.fromDesc(dto.getEmoji());
            newLog.setEmoji(emoji);
        } else {
            // 未传则使用默认表情
            newLog.setEmoji(Emoji.PEACE);
        }
        newLog.setCreatedTime(LocalDateTime.now());

        // 第一次保存 (获取 logId)
        Log savedLog = logRepository.save(newLog);

        // 3. 遍历并保存所有附件
        if (files != null && files.length > 0) {
            for (MultipartFile file : files) {

                if (file.isEmpty())
                    continue;

                try {
                    // 3a. 存到磁盘 (Old)
                    // String storedFilename = fileStorageService.storeFile(file);

                    // 3a. 存到 OSS (New)
                    String storedFilename = ossService.uploadFile(file);

                    // 3b. 存 "信息" 到数据库
                    LogAttachment attachment = new LogAttachment();
                    attachment.setLog(savedLog); // 关联日志
                    attachment.setOriginalFilename(file.getOriginalFilename());
                    attachment.setStoredFilename(storedFilename);
                    attachment.setFileType(file.getContentType());
                    attachment.setFileSize(file.getSize());
                    attachment.setUploadedBy(userId); // 设置上传者ID
                    // uploadTime 会通过 @PrePersist 自动设置

                    logAttachmentRepository.save(attachment);

                } catch (IOException e) {
                    throw new RuntimeException("文件上传失败: " + file.getOriginalFilename(), e);
                }
            }
        }
        return savedLog;
    }

    /**
     * 根据任务ID查询所有日志
     * 
     * @param taskId 任务ID
     * @return 任务所有日志的 DTO 列表
     */

    public List<LogDTO> getLogsByTask(Integer taskId) {
        List<Log> logs = logRepository.findByTask_TaskId(taskId);
        return convertToDtoList(logs);
    }

    /**
     * 根据用户ID和日期查询所有日志
     * 
     * @param userId   用户ID
     * @param dateTime 日期时间
     * @return 该用户在指定日期的所有日志的 DTO 列表
     */
    public List<LogDTO> getLogsByDate(Integer userId, LocalDateTime dateTime) {
        LocalDateTime startOfDay = dateTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = dateTime.toLocalDate().atTime(LocalTime.MAX);
        List<Log> logs = logRepository.findByEmployeeEmployeeIdAndCreatedTimeBetween(userId, startOfDay, endOfDay);
        return convertToDtoList(logs);
    }

    public List<LogDTO> getAllLogs() {
        List<Log> logs = logRepository.findAllWithDetails(); // 假设这个方法做了 JOIN FETCH
        return convertToDtoList(logs);
    }

    /**
     * 搜索日志
     * 只返回创建人与当前员工ID相同的日志
     * 
     * @param keyword 搜索关键词（可为空）
     * @param userId  当前用户ID
     * @return 符合条件的日志列表
     */
    public List<LogDTO> searchLogs(String keyword, Integer userId) {
        // 如果 keyword 为空，返回空列表
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 搜索所有匹配的日志
        List<Log> logs = logRepository.searchByKeyword(keyword.trim());

        // 过滤：只返回创建人与员工ID相同的日志
        List<Log> filteredLogs = logs.stream()
                .filter(log -> log.getEmployee() != null
                        && log.getEmployee().getEmployeeId().equals(userId))
                .collect(Collectors.toList());

        return convertToDtoList(filteredLogs);
    }

    /**
     * 根据ID查询单个日志 (已优化)
     * 
     * @param logId 日志ID
     * @return 日志的DTO表示
     */
    public LogDTO getLogById(Integer logId) {
        Log log = logRepository.findByIdWithDetails(logId) // 假设这个方法做了 JOIN FETCH
                .orElseThrow(() -> new RuntimeException("Log not found with id: " + logId));
        return convertToDto(log);
    }

    @Transactional
    public LogDTO updateLog(Integer id, LogDTO logDTO) {
        Log existingLog = logRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Log not found with id: " + id));
        existingLog.setContent(logDTO.getContent());
        if (logDTO.getEmoji() != null) {
            existingLog.setEmoji(Emoji.fromDesc(logDTO.getEmoji()));
        }
        existingLog.setEmployeeLocation(logDTO.getEmployeeLocation());

        // (注意：附件的更新逻辑很复杂，通常是单独的接口)
        // (这个 updateLog 暂不处理附件的增删)

        if (logDTO.getTaskId() != null) {
            Task task = taskRepository.findById(logDTO.getTaskId())
                    .orElseThrow(() -> new RuntimeException("Task not found with id: " + logDTO.getTaskId()));
            existingLog.setTask(task);
        } else {
            existingLog.setTask(null);
        }

        Log savedLog = logRepository.save(existingLog);
        return convertToDto(savedLog); // 返回转换后的 DTO
    }

    public void deleteLog(Integer id) {
        if (!logRepository.existsById(id)) {
            throw new RuntimeException("Log not found with id: " + id);
        }
        logRepository.deleteById(id);
    }

    public List<LogDTO> queryLogsInWeek(Integer userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = (endDate == null ? startDate.plusDays(6) : endDate).atTime(LocalTime.MAX);

        List<Log> logs = logRepository.findByEmployeeEmployeeIdAndCreatedTimeBetween(userId, startDateTime,
                endDateTime);
        return convertToDtoList(logs);
    }

    public List<LogDTO> queryLogsInMonth(Integer userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endDateTime = (endDate == null ? startDate.withDayOfMonth(startDate.lengthOfMonth()) : endDate)
                .atTime(LocalTime.MAX);

        List<Log> logs = logRepository.findByEmployeeEmployeeIdAndCreatedTimeBetween(userId, startDateTime,
                endDateTime);
        return convertToDtoList(logs);
    }

    public LogDTO getDetailLog(Integer logId) {
        return getLogById(logId);
    }

    // ==================================================
    // 辅助方法 (已修复)
    // ==================================================

    private List<LogDTO> convertToDtoList(List<Log> logs) {
        return logs.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * 将单个 Log 实体转换为 LogDTO 的通用方法
     * (已修复：使用 log.getAttachments() )
     */
    private LogDTO convertToDto(Log log) {
        LogDTO dto = new LogDTO();
        dto.setLogId(log.getLogId());

        if (log.getEmployee() != null) {
            dto.setEmployeeName(log.getEmployee().getEmployeeName());
            dto.setEmployeeId(log.getEmployee().getEmployeeId());
        }

        if (log.getTask() != null) {
            dto.setTaskName(log.getTask().getTitle());
            dto.setTaskId(log.getTask().getTaskId());
        }

        dto.setCreatedTime(log.getCreatedTime());
        dto.setContent(log.getContent());
        dto.setEmoji(log.getEmoji().getDesc());
        dto.setEmployeeLocation(log.getEmployeeLocation());

        // --- 核心修复 ---
        // 转换附件集合
        if (log.getAttachments() != null && !log.getAttachments().isEmpty()) {
            Set<AttachmentDTO> attachmentDTOs = log.getAttachments().stream()
                    .map(att -> {
                        AttachmentDTO attDto = new AttachmentDTO();
                        attDto.setId(att.getId());
                        attDto.setOriginalFilename(att.getOriginalFilename());
                        attDto.setFileType(att.getFileType());
                        attDto.setFileSize(att.getFileSize());
                        attDto.setUploadTime(att.getUploadTime());
                        return attDto;
                    })
                    .collect(Collectors.toSet());
            dto.setAttachments(attachmentDTOs);
        }
        // --- 修复结束 ---
        return dto;
    }
}