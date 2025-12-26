package com.pandora.backend.agent.service;

import com.pandora.backend.agent.model.WorkReportTrendSummary;
import com.pandora.backend.entity.Log;
import com.pandora.backend.entity.Task;
import com.pandora.backend.enums.Emoji;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class WorkReportTrendExtractor {

    private static final int PERCENT_BASE = 100;
    private static final int MAX_LATEST_LOG_PREVIEW_LENGTH = 60;

    private WorkReportTrendExtractor() {
    }

    public static WorkReportTrendSummary extract(
            final LocalDateTime periodStart,
            final LocalDateTime periodEnd,
            final List<Log> logs,
            final List<Task> tasks) {
        final String workTrend = buildWorkTrend(periodStart, periodEnd, logs, tasks);
        final String emotionTrend = buildEmotionTrend(logs);
        final String taskTrend = buildTaskTrend(tasks);
        return new WorkReportTrendSummary(workTrend, emotionTrend, taskTrend);
    }

    private static String buildWorkTrend(
            final LocalDateTime periodStart,
            final LocalDateTime periodEnd,
            final List<Log> logs,
            final List<Task> tasks) {
        final int logCount = logs.size();
        final int taskCount = tasks.size();

        if (logCount == 0 && taskCount == 0) {
            return "近三周没有找到日志或任务数据。";
        }

        final long activeLogDays = logs.stream()
                .map(Log::getCreatedTime)
                .filter(time -> time != null)
                .map(LocalDateTime::toLocalDate)
                .distinct()
                .count();

        final String latestLogPreview = logs.stream()
                .filter(log -> log.getCreatedTime() != null)
                .sorted((l1, l2) -> l2.getCreatedTime().compareTo(l1.getCreatedTime()))
                .map(Log::getContent)
                .filter(content -> content != null && !content.isBlank())
                .map(WorkReportTrendExtractor::truncateLogContent)
                .findFirst()
                .orElse("(无)");

        final long logsWithTask = logs.stream()
                .filter(log -> log.getTask() != null)
                .count();

        return "周期 " + periodStart.toLocalDate() + " ~ " + periodEnd.toLocalDate()
                + "：日志=" + logCount
                + "(活跃天数=" + activeLogDays + ")"
                + "，任务=" + taskCount
                + "，日志关联任务=" + logsWithTask
                + "。最近一条日志摘要：" + latestLogPreview;
    }

    private static String truncateLogContent(final String content) {
        if (content == null) {
            return "(无)";
        }

        final String normalized = content
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();

        if (normalized.length() <= MAX_LATEST_LOG_PREVIEW_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_LATEST_LOG_PREVIEW_LENGTH) + "...";
    }

    private static String buildEmotionTrend(final List<Log> logs) {
        if (logs.isEmpty()) {
            return "近三周没有日志，无法推断情绪趋势。";
        }

        final Map<Emoji, Integer> counts = new EnumMap<>(Emoji.class);
        for (Emoji emoji : Emoji.values()) {
            counts.put(emoji, 0);
        }

        for (Log log : logs) {
            final Emoji emoji = log.getEmoji() == null ? Emoji.PEACE : log.getEmoji();
            counts.put(emoji, counts.get(emoji) + 1);
        }

        final int total = logs.size();
        final int pressureCount = counts.getOrDefault(Emoji.PRESSURE, 0);
        final int fatigueCount = counts.getOrDefault(Emoji.FATIGUE, 0);
        final int angryCount = counts.getOrDefault(Emoji.ANGRY, 0);
        final int negative = pressureCount + fatigueCount + angryCount;
        final int negativePercent = total == 0 ? 0 : (negative * PERCENT_BASE) / total;

        return "情绪分布：开心=" + counts.getOrDefault(Emoji.HAPPY, 0)
                + "，压力=" + pressureCount
                + "，平静=" + counts.getOrDefault(Emoji.PEACE, 0)
                + "，疲惫=" + fatigueCount
                + "，生气=" + angryCount
                + "，负面占比=" + negativePercent + "%";
    }

    private static String buildTaskTrend(final List<Task> tasks) {
        if (tasks.isEmpty()) {
            return "近三周没有任务数据，无法推断任务趋势。";
        }

        int completed = 0;
        int inProgressOrTodo = 0;
        int urgentOrHigh = 0;

        for (Task task : tasks) {
            final Byte status = task.getTaskStatus();
            if (status != null && status == 2) {
                completed += 1;
            } else {
                inProgressOrTodo += 1;
            }

            final Byte priority = task.getTaskPriority();
            if (priority != null && priority >= 2) {
                urgentOrHigh += 1;
            }
        }

        final int total = tasks.size();
        final int completedPercent = total == 0 ? 0 : (completed * PERCENT_BASE) / total;
        return "任务完成=" + completed + "/" + total + "(" + completedPercent + "%)"
                + "，未完成=" + inProgressOrTodo
                + "，高优先级=" + urgentOrHigh;
    }
}
