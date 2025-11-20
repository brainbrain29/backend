package com.pandora.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * 员工一周心情统计 DTO
 * 用于前端绘制心情分布图
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoodStatisticsDTO {

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
     * 本周日志总数
     */
    private Integer totalLogs;

    /**
     * 心情分布统计
     * Key: 心情名称（如 "开心", "压力", "平静", "疲惫", "生气"）
     * Value: MoodCount 对象，包含数量和占比
     */
    private Map<String, MoodCount> moodDistribution;

    /**
     * 心情统计内部类
     * 包含某种心情的数量和占比
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MoodCount {
        /**
         * 该心情的数量
         */
        private Integer count;

        /**
         * 该心情的占比（0-100）
         */
        private Double percentage;
    }
}
