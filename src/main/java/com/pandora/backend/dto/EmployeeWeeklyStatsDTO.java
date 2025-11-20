package com.pandora.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 员工本周统计数据 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeWeeklyStatsDTO {

    /**
     * 员工ID
     */
    private Integer employeeId;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 本周开始日期（周一）
     */
    private LocalDate weekStart;

    /**
     * 本周结束日期（周日）
     */
    private LocalDate weekEnd;

    /**
     * 本周日志数
     */
    private Integer logCount;

    /**
     * 本周完成任务数
     */
    private Integer completedTaskCount;

    /**
     * 本周总任务数
     */
    private Integer totalTaskCount;

    /**
     * 任务完成率 (%)
     */
    private Double completionRate;
}
