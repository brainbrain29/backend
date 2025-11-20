package com.pandora.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 员工一周每日工作量 DTO
 * 用于前端绘制工作量趋势图
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyWorkloadDTO {

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
     * 每日工作量数据（一周7天）
     */
    private List<DailyData> dailyData;

    /**
     * 每日数据内部类
     * 包含某一天的日志数和任务数
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyData {
        /**
         * 日期
         */
        private LocalDate date;

        /**
         * 星期几（英文，如 "Monday", "Tuesday"）
         */
        private String dayOfWeek;

        /**
         * 当天创建的日志数
         */
        private Integer logCount;

        /**
         * 当天创建的任务数
         */
        private Integer taskCount;
    }
}
