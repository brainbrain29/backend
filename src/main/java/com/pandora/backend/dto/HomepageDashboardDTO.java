package com.pandora.backend.dto;

import lombok.Data;

import java.util.List;

// in dto package
@Data
public class HomepageDashboardDTO {
    private List<ImportantMatterDTO> companyNotices; // 公司十大事项
    private List<ImportantTaskDTO> companyTasks; // 公司十大任务
    private List<TaskSummaryDTO> personalTasks; // 个人十大任务
    private List<LogSummaryDTO> todayLogs; // 今日日志
}