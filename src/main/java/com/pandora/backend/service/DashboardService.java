package com.pandora.backend.service;

import com.pandora.backend.dto.HomepageDashboardDTO;
import com.pandora.backend.dto.ImportantMatterDTO;
import com.pandora.backend.dto.ImportantTaskDTO;
import com.pandora.backend.dto.LogSummaryDTO;
import com.pandora.backend.dto.TaskSummaryDTO;
import com.pandora.backend.enums.Status;
import com.pandora.backend.enums.Priority;
import com.pandora.backend.entity.ImportantMatter;
import com.pandora.backend.entity.ImportantTask;
import com.pandora.backend.entity.Log;
import com.pandora.backend.entity.Task;
import com.pandora.backend.repository.ImportantMatterRepository;
import com.pandora.backend.repository.ImportantTaskRepository;
import com.pandora.backend.repository.LogRepository;
import com.pandora.backend.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private ImportantMatterRepository importantMatterRepository;
    @Autowired
    private ImportantTaskRepository importantTaskRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private LogRepository logRepository;

    public HomepageDashboardDTO getDashboardData(Integer currentUserId) {
        Pageable topTen = PageRequest.of(0, 10);

        // 1. 获取公司十大事项
        List<ImportantMatterDTO> importantMatters = importantMatterRepository.findTopMatters(topTen)
                .stream().map(this::convertToImportantMatterDto).collect(Collectors.toList());

        // 2. 获取公司十大任务
        List<ImportantTaskDTO> companyTasks = importantTaskRepository.findTopTasks(topTen)
                .stream().map(this::convertToImportantTaskDto).collect(Collectors.toList());

        // 3. 获取个人十大任务
        List<TaskSummaryDTO> personalTasks = taskRepository.findTop10PersonalTasks(currentUserId, topTen)
                .stream().map(this::convertToTaskSummaryDto).collect(Collectors.toList());

        // 4. 获取今日日志
        List<LogSummaryDTO> todayLogs = logRepository
                .findTodayLogsByEmployeeId(currentUserId, LocalDate.now().atStartOfDay(),
                        LocalDate.now().plusDays(1).atStartOfDay())
                .stream().map(this::convertToLogSummaryDto).collect(Collectors.toList());

        // 组装最终的 DTO
        HomepageDashboardDTO dashboard = new HomepageDashboardDTO();
        dashboard.setCompanyNotices(importantMatters);
        dashboard.setCompanyTasks(companyTasks);
        dashboard.setPersonalTasks(personalTasks);
        dashboard.setTodayLogs(todayLogs);

        return dashboard;
    }

    // ==============================================================
    // ==== 辅助转换方法 (已根据你的实体类完全修正) ====
    // ==============================================================

    private ImportantMatterDTO convertToImportantMatterDto(ImportantMatter matter) {
        ImportantMatterDTO dto = new ImportantMatterDTO();
        dto.setEventId(matter.getMatterId());
        dto.setTitle(matter.getTitle());
        dto.setContent(matter.getContent());

        // 设置部门名称
        if (matter.getDepartment() != null) {
            dto.setDepartmentName(matter.getDepartment().getOrgName());
        }

        // 设置发布时间
        dto.setPublishTime(matter.getPublishTime());

        return dto;
    }

    private ImportantTaskDTO convertToImportantTaskDto(ImportantTask task) {
        ImportantTaskDTO dto = new ImportantTaskDTO();
        dto.setTaskId(task.getTaskId());
        dto.setTaskContent(task.getTaskContent());
        dto.setDeadline(task.getDeadline());
        dto.setSerialNum(task.getSerialNum());

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

    private TaskSummaryDTO convertToTaskSummaryDto(Task task) {
        TaskSummaryDTO dto = new TaskSummaryDTO();
        dto.setTaskId(task.getTaskId());
        dto.setTitle(task.getTitle());

        // 将 code 转换为中文描述
        if (task.getTaskPriority() != null) {
            Priority priority = Priority.fromCode(task.getTaskPriority());
            dto.setTaskPriority(priority.getDesc());
        }
        if (task.getTaskStatus() != null) {
            Status status = Status.fromCode(task.getTaskStatus());
            dto.setTaskStatus(status.getDesc());
        }

        if (task.getAssignee() != null) {
            dto.setAssigneeName(task.getAssignee().getEmployeeName());
        }
        if (task.getEndTime() != null) {
            dto.setDueDate(task.getEndTime().toLocalDate());
        }
        return dto;
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private LogSummaryDTO convertToLogSummaryDto(Log log) {
        LogSummaryDTO dto = new LogSummaryDTO();
        dto.setLogId(log.getLogId());
        dto.setCreatedTime(log.getCreatedTime());
        // 内容摘要：截取内容的前 50 个字符
        if (log.getContent() != null && log.getContent().length() > 50) {
            dto.setContentSummary(log.getContent().substring(0, 50) + "...");
        } else {
            dto.setContentSummary(log.getContent());
        }
        return dto;
    }
}