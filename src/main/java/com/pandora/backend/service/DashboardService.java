package com.pandora.backend.service;

import com.pandora.backend.dto.HomepageDashboardDTO;
import com.pandora.backend.dto.LogSummaryDTO;
import com.pandora.backend.dto.NoticeSummaryDTO;
import com.pandora.backend.dto.TaskSummaryDTO;
import com.pandora.backend.entity.Log;
import com.pandora.backend.entity.Notice;
import com.pandora.backend.entity.Task;
import com.pandora.backend.repository.LogRepository;
import com.pandora.backend.repository.NoticeRepository;
import com.pandora.backend.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    @Autowired
    private NoticeRepository noticeRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private LogRepository logRepository;

    public HomepageDashboardDTO getDashboardData(Integer currentUserId) {
        Pageable topTen = PageRequest.of(0, 10);

        // 1. 获取公司十大事项
        List<NoticeSummaryDTO> notices = noticeRepository.findTop10Notices(topTen)
                .stream().map(this::convertToNoticeSummaryDto).collect(Collectors.toList());

        // 2. 获取公司十大任务
        List<TaskSummaryDTO> companyTasks = taskRepository.findTop10CompanyTasks(topTen)
                .stream().map(this::convertToTaskSummaryDto).collect(Collectors.toList());

        // 3. 获取个人十大任务
        List<TaskSummaryDTO> personalTasks = taskRepository.findTop10PersonalTasks(currentUserId, topTen)
                .stream().map(this::convertToTaskSummaryDto).collect(Collectors.toList());

        // 4. 获取今日日志
        List<LogSummaryDTO> todayLogs = logRepository.findTodayLogsByEmployeeId(currentUserId, LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay())
                .stream().map(this::convertToLogSummaryDto).collect(Collectors.toList());

        // 组装最终的 DTO
        HomepageDashboardDTO dashboard = new HomepageDashboardDTO();
        dashboard.setCompanyNotices(notices);
        dashboard.setCompanyTasks(companyTasks);
        dashboard.setPersonalTasks(personalTasks);
        dashboard.setTodayLogs(todayLogs);

        return dashboard;
    }

    // ==============================================================
    // ==== 辅助转换方法 (已根据你的实体类完全修正) ====
    // ==============================================================

    private NoticeSummaryDTO convertToNoticeSummaryDto(Notice notice) {
        NoticeSummaryDTO dto = new NoticeSummaryDTO();
        dto.setId(notice.getNoticeId());

        String rawContent = notice.getContent();
        if (rawContent == null || rawContent.isBlank()) {
            dto.setTitle("公告");
            dto.setSummary(null);
        } else {
            String content = rawContent.trim();
            dto.setTitle(abbreviate(content, 20));
            dto.setSummary(abbreviate(content, 50));
        }

        Byte noticeType = notice.getNoticeType();
        dto.setTag(noticeType != null ? String.valueOf(noticeType) : null);

        dto.setPublishTime(notice.getCreatedTime());

        dto.setUnread(false);

        return dto;
    }

    private TaskSummaryDTO convertToTaskSummaryDto(Task task) {
        TaskSummaryDTO dto = new TaskSummaryDTO();
        dto.setTaskId(task.getTaskId());
        dto.setTitle(task.getTitle());
        dto.setPriority(task.getTaskPriority());
        dto.setStatus(task.getTaskStatus());
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