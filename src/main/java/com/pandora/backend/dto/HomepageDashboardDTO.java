package com.pandora.backend.dto;

import lombok.Data;

import java.util.List;

// in dto package
@Data
public class HomepageDashboardDTO {
    private List<NoticeSummaryDTO> companyNotices;
    private List<TaskSummaryDTO> companyTasks;
    private List<TaskSummaryDTO> personalTasks;
    private List<LogSummaryDTO> todayLogs;
}