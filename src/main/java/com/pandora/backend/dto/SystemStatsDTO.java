package com.pandora.backend.dto;

import lombok.Data;

@Data
public class SystemStatsDTO {
    private Integer activeUsers;           // 活跃用户数（员工总数）
    private Integer inProgressTasks;       // 进行中任务数
    private Integer todayDeliveries;       // 今日到期任务数
    private Double completionRate;         // 任务完成率
}
