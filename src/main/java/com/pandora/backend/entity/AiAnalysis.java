package com.pandora.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * AI 分析结果实体类
 * 用于存储每周生成的 AI 工作分析报告
 */
@Getter
@Setter
@Entity
@Table(name = "ai_analysis")
public class AiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Integer analysisId;

    /**
     * 关联的员工
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /**
     * 分析生成时间
     */
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;

    /**
     * 分析周期开始时间（近三周的起始时间）
     */
    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    /**
     * 分析周期结束时间
     */
    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    /**
     * 工作节奏建议（💡）
     */
    @Column(name = "work_rhythm_advice", columnDefinition = "TEXT")
    private String workRhythmAdvice;

    /**
     * 情绪健康提醒（⚠️）
     */
    @Column(name = "emotion_health_reminder", columnDefinition = "TEXT")
    private String emotionHealthReminder;

    /**
     * 任务完成趋势（ℹ️）
     */
    @Column(name = "task_completion_trend", columnDefinition = "TEXT")
    private String taskCompletionTrend;

    /**
     * 完整的 AI 回复内容（包含三个主题）
     */
    @Column(name = "full_content", columnDefinition = "TEXT")
    private String fullContent;

    /**
     * 分析时使用的日志数量
     */
    @Column(name = "log_count")
    private Integer logCount;

    /**
     * 分析时使用的任务数量
     */
    @Column(name = "task_count")
    private Integer taskCount;
}
