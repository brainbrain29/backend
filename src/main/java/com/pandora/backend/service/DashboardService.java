package com.pandora.backend.service;

import com.pandora.backend.dto.HomepageDashboardDTO;
import com.pandora.backend.dto.ImportantMatterDTO;
import com.pandora.backend.dto.ImportantTaskDTO;
import com.pandora.backend.dto.LogDTO;
import com.pandora.backend.dto.TaskDTO;
import com.pandora.backend.entity.ImportantMatter;
import com.pandora.backend.entity.ImportantTask;
import com.pandora.backend.repository.ImportantMatterRepository;
import com.pandora.backend.repository.ImportantTaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DashboardService {

    @Autowired
    private ImportantMatterRepository importantMatterRepository;
    @Autowired
    private ImportantTaskRepository importantTaskRepository;
    @Autowired
    private TaskService taskService;
    @Autowired
    private LogService logService;

    public HomepageDashboardDTO getDashboardData(Integer currentUserId) {
        log.info("获取用户 {} 的首页仪表盘数据", currentUserId);
        Pageable topTen = PageRequest.of(0, 10);

        // 1. 获取公司十大事项
        List<ImportantMatterDTO> importantMatters = importantMatterRepository.findTopMatters(topTen)
                .stream().map(this::convertToImportantMatterDto).collect(Collectors.toList());
        log.debug("公司事项数量: {}", importantMatters.size());

        // 2. 获取公司十大任务
        List<ImportantTaskDTO> companyTasks = importantTaskRepository.findTopTasks(topTen)
                .stream().map(this::convertToImportantTaskDto).collect(Collectors.toList());
        log.debug("公司任务数量: {}", companyTasks.size());

        // 3. 复用 TaskService.getTasksByUserId() - 获取个人任务,然后转换为 Summary 格式
        List<TaskDTO> personalTasksFullDTO = taskService.getTasksByUserId(currentUserId);
        List<com.pandora.backend.dto.TaskSummaryDTO> personalTasks = personalTasksFullDTO.stream()
                .map(this::convertTaskDTOToSummary)
                .collect(Collectors.toList());
        log.debug("用户 {} 的个人任务数量: {}", currentUserId, personalTasks.size());

        // 4. 复用 LogService.getLogsByDate() - 获取今日日志,然后转换为 Summary 格式
        LocalDateTime today = LocalDate.now().atStartOfDay();
        List<LogDTO> todayLogsFullDTO = logService.getLogsByDate(currentUserId, today);
        List<com.pandora.backend.dto.LogSummaryDTO> todayLogs = todayLogsFullDTO.stream()
                .map(this::convertLogDTOToSummary)
                .collect(Collectors.toList());
        log.debug("用户 {} 的今日日志数量: {}", currentUserId, todayLogs.size());

        // 组装最终的 DTO
        HomepageDashboardDTO dashboard = new HomepageDashboardDTO();
        dashboard.setCompanyNotices(importantMatters);
        dashboard.setCompanyTasks(companyTasks);
        dashboard.setPersonalTasks(personalTasks);
        dashboard.setTodayLogs(todayLogs);
        return dashboard;
    }

    /**
     * Get important matter by ID
     * 
     * @param matterId the matter ID
     * @return ImportantMatterDTO or null if not found
     */
    public ImportantMatterDTO getImportantMatterById(Integer matterId) {
        log.info("获取重要事项,ID: {}", matterId);
        return importantMatterRepository.findById(matterId)
                .map(this::convertToImportantMatterDto)
                .orElse(null);
    }

    /**
     * Get important task by ID
     * 
     * @param taskId the task ID
     * @return ImportantTaskDTO or null if not found
     */
    public ImportantTaskDTO getImportantTaskById(Integer taskId) {
        log.info("获取重要任务,ID: {}", taskId);
        return importantTaskRepository.findById(taskId)
                .map(this::convertToImportantTaskDto)
                .orElse(null);
    }

    // ==============================================================
    // ==== 辅助转换方法 (已根据你的实体类完全修正) ====
    // ==============================================================

    private ImportantMatterDTO convertToImportantMatterDto(ImportantMatter matter) {
        ImportantMatterDTO dto = new ImportantMatterDTO();
        // 问题(1): 设置 matterId
        dto.setMatterId(matter.getMatterId());
        dto.setTitle(matter.getTitle());
        dto.setContent(matter.getContent());

        // 设置部门名称
        if (matter.getDepartment() != null) {
            dto.setDepartmentId(matter.getDepartment().getOrgId());
            dto.setDepartmentName(matter.getDepartment().getOrgName());
        }

        // 设置发布时间
        dto.setPublishTime(matter.getPublishTime());

        // 添加默认值以兼容前端模板
        dto.setDeadline(matter.getPublishTime()); // 使用发布时间作为截止日期
        dto.setAssigneeName("系统"); // 默认负责人
        dto.setAssigneeId(1); // 默认负责人ID
        dto.setMatterStatus((byte) 0); // 默认状态：待处理
        dto.setMatterPriority((byte) 1); // 默认优先级：中
        dto.setSerialNum((byte) 1); // 默认序号
        dto.setVisibleRange(0); // 默认可见范围

        return dto;
    }

    private ImportantTaskDTO convertToImportantTaskDto(ImportantTask task) {
        ImportantTaskDTO dto = new ImportantTaskDTO();
        dto.setTaskId(task.getTaskId());
        dto.setTaskContent(task.getTaskContent());
        dto.setDeadline(task.getDeadline());
        dto.setSerialNum(task.getSerialNum());

        // 问题(2): 设置 createdTime 和 updatedTime
        dto.setCreatedTime(task.getCreatedTime());
        dto.setUpdatedTime(task.getUpdatedTime());

        // 设置员工信息
        if (task.getEmployee() != null) {
            dto.setEmployeeId(task.getEmployee().getEmployeeId());
            dto.setEmployeeName(task.getEmployee().getEmployeeName());
        }

        // 将 code 转换为中文描述
        if (task.getTaskStatus() != null) {
            // 0: 待处理, 1: 进行中, 2: 已完成
            String statusDesc = switch (task.getTaskStatus()) {
                case 0 -> "待处理";
                case 1 -> "进行中";
                case 2 -> "已完成";
                default -> "未知";
            };
            dto.setTaskStatus(statusDesc);
        }
        if (task.getTaskPriority() != null) {
            // 0: 低, 1: 中, 2: 高
            String priorityDesc = switch (task.getTaskPriority()) {
                case 0 -> "低";
                case 1 -> "中";
                case 2 -> "高";
                default -> "未知";
            };
            dto.setTaskPriority(priorityDesc);
        }

        return dto;
    }

    /**
     * 将完整的 TaskDTO 转换为 TaskSummaryDTO（用于仪表盘展示）
     */
    private com.pandora.backend.dto.TaskSummaryDTO convertTaskDTOToSummary(TaskDTO taskDTO) {
        com.pandora.backend.dto.TaskSummaryDTO summary = new com.pandora.backend.dto.TaskSummaryDTO();
        summary.setTaskId(taskDTO.getTaskId());
        summary.setTitle(taskDTO.getTitle());
        summary.setTaskPriority(taskDTO.getTaskPriority());
        summary.setTaskStatus(taskDTO.getTaskStatus());
        summary.setAssigneeName(taskDTO.getAssigneeName());
        
        // 将 LocalDateTime 转换为 LocalDate
        if (taskDTO.getEndTime() != null) {
            summary.setDueDate(taskDTO.getEndTime().toLocalDate());
        }
        
        return summary;
    }

    /**
     * 将完整的 LogDTO 转换为 LogSummaryDTO（用于仪表盘展示）
     */
    private com.pandora.backend.dto.LogSummaryDTO convertLogDTOToSummary(LogDTO logDTO) {
        com.pandora.backend.dto.LogSummaryDTO summary = new com.pandora.backend.dto.LogSummaryDTO();
        summary.setLogId(logDTO.getLogId());
        summary.setCreatedTime(logDTO.getCreatedTime());
        
        // 内容摘要：截取内容的前 50 个字符
        String content = logDTO.getContent();
        if (content != null && content.length() > 50) {
            summary.setContentSummary(content.substring(0, 50) + "...");
        } else {
            summary.setContentSummary(content);
        }
        
        return summary;
    }
}