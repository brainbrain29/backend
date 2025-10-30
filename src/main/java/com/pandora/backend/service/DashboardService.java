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
        // 你的 Notice 实体没有 title，但有 content，我们用 content 作为标题
        dto.setTitle(notice.getContent());

        // 摘要: 截取 content 的前 30 个字符作为摘要
        if (notice.getContent() != null && notice.getContent().length() > 30) {
            dto.setSummary(notice.getContent().substring(0, 30) + "...");
        } else {
            dto.setSummary(notice.getContent());
        }

        // 你的 Notice 实体没有 tag，可以暂时设为 null 或根据 noticeType 判断
        // dto.setTag(String.valueOf(notice.getNoticeType())); // 比如将数字类型转为字符串
        dto.setTag("公告"); // 或者先写死一个

        dto.setPublishTime(notice.getCreatedTime());

        // 未读状态的逻辑需要专门实现，比如通过 Notice_Employee 这个中间表来判断
        dto.setUnread(true); // 暂时写死为 true

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