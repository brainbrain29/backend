package com.pandora.backend.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandora.backend.agent.constants.AgentMemoryType;
import com.pandora.backend.agent.model.AttachmentInsight;
import com.pandora.backend.agent.model.AttachmentMeta;
import com.pandora.backend.agent.model.WorkReportTrendSummary;
import com.pandora.backend.config.GlmConfig;
import com.pandora.backend.dto.ChatMessageDTO;
import com.pandora.backend.dto.ChatRequestDTO;
import com.pandora.backend.entity.AiAnalysis;
import com.pandora.backend.entity.AgentMemory;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.Log;
import com.pandora.backend.entity.Project;
import com.pandora.backend.entity.Task;
import com.pandora.backend.repository.AiAnalysisRepository;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.repository.LogRepository;
import com.pandora.backend.repository.ProjectRepository;
import com.pandora.backend.repository.TaskRepository;
import com.pandora.backend.service.AgentMemoryService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkReportAgentService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int LOG_LIMIT = 50;
    private static final int TASK_LIMIT = 30;

    private final GlmConfig glmConfig;
    private final ObjectMapper objectMapper;

    private final LogRepository logRepository;
    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final ProjectRepository projectRepository;

    private final AttachmentInsightService attachmentInsightService;
    private final AgentMemoryService agentMemoryService;
    private final EntityManager entityManager;

    private record ChatContextData(
            Employee employee,
            List<Log> logs,
            List<Task> tasks,
            List<Project> projects,
            List<AgentMemory> memories,
            List<AttachmentMeta> attachmentMetas,
            List<AttachmentInsight> attachmentInsights,
            WorkReportTrendSummary trendSummary) {
    }

    public void generateWorkReport(final Integer userId, final SseEmitter emitter) {
        log.info("[agent-plan] start generateWorkReport userId={}", userId);
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime periodStart = now.minusWeeks(3);

        final Employee employee = employeeRepository.findById(userId).orElse(null);
        final List<Log> logs = safeLoadLogs(userId, periodStart, now);
        final List<Task> tasks = safeLoadTasks(userId, periodStart);
        final List<Project> projects = safeLoadProjects(employee, userId);

        final Integer firstLogId = logs.isEmpty() ? null : logs.get(0).getLogId();
        final boolean firstLogManaged = logs.isEmpty() ? false : entityManager.contains(logs.get(0));
        log.info(
                "[agent-plan] loaded data userId={} employeeFound={} logs={} tasks={} projects={} firstLogId={} firstLogManaged={}",
                userId,
                employee != null,
                logs.size(),
                tasks.size(),
                projects.size(),
                firstLogId,
                firstLogManaged);

        final List<AgentMemory> memories = agentMemoryService.getLatestMemories(userId);
        log.info("[agent-plan] loaded memories userId={} memories={}", userId, memories.size());

        final List<AttachmentMeta> attachmentMetas = loadAttachmentMetas(userId, logs, tasks);
        final List<AttachmentInsight> attachmentInsights = decideAndAnalyzeAttachments(logs, tasks, attachmentMetas);

        log.info("[agent-plan] attachments userId={} metas={} insights={}", userId, attachmentMetas.size(),
                attachmentInsights.size());

        final WorkReportTrendSummary trendSummary = WorkReportTrendExtractor.extract(periodStart, now, logs, tasks);

        log.info("[agent-plan] trend extracted userId={} work='{}' emotion='{}' task='{}'", userId,
                trendSummary.workTrend(), trendSummary.emotionTrend(), trendSummary.taskTrend());

        log.info("[agent-plan] entityManager.clear() before upsertMemories userId={}", userId);
        entityManager.clear();

        final boolean firstLogManagedAfterClear = logs.isEmpty() ? false : entityManager.contains(logs.get(0));
        log.info("[agent-plan] entityManager.clear() done userId={} firstLogManagedAfterClear={}", userId,
                firstLogManagedAfterClear);

        upsertMemories(userId, trendSummary);
        log.info("[agent-plan] upsertMemories done userId={}", userId);

        final List<ChatMessageDTO> messages = buildPrompt(employee, logs, tasks, attachmentMetas, attachmentInsights,
                projects, memories, trendSummary);
        log.info("[agent-plan] prompt built userId={} messages={}", userId, messages.size());
        chatStreamAndSave(messages, emitter, userId, periodStart, now, logs.size(), tasks.size());
        log.info("[agent-plan] chatStream started userId={}", userId);
    }

    public List<ChatMessageDTO> buildChatContextMessages(
            final Integer userId,
            final int weeks,
            final boolean includeAttachments) {
        final ChatContextData data = loadChatContextData(userId, weeks, includeAttachments);
        return buildChatContextPrompt(data);
    }

    private ChatContextData loadChatContextData(
            final Integer userId,
            final int weeks,
            final boolean includeAttachments) {
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime periodStart = now.minusWeeks(weeks);

        final Employee employee = employeeRepository.findById(userId).orElse(null);
        final List<Log> logs = safeLoadLogs(userId, periodStart, now);
        final List<Task> tasks = safeLoadTasks(userId, periodStart);
        final List<Project> projects = safeLoadProjects(employee, userId);
        final List<AgentMemory> memories = agentMemoryService.getLatestMemories(userId);

        final List<AttachmentMeta> attachmentMetas;
        final List<AttachmentInsight> attachmentInsights;
        if (includeAttachments) {
            attachmentMetas = loadAttachmentMetas(userId, logs, tasks);
            attachmentInsights = decideAndAnalyzeAttachments(logs, tasks, attachmentMetas);
        } else {
            attachmentMetas = List.of();
            attachmentInsights = List.of();
        }

        final WorkReportTrendSummary trendSummary = WorkReportTrendExtractor.extract(periodStart, now, logs, tasks);
        return new ChatContextData(employee, logs, tasks, projects, memories, attachmentMetas, attachmentInsights,
                trendSummary);
    }

    private List<ChatMessageDTO> buildChatContextPrompt(final ChatContextData data) {
        final List<ChatMessageDTO> messages = new ArrayList<>();

        messages.add(new ChatMessageDTO("system", "Context for user work history. Use it to answer questions."));

        if (data.employee() != null
                && data.employee().getMbti() != null
                && !data.employee().getMbti().isEmpty()) {
            messages.add(new ChatMessageDTO("system", "User MBTI: " + data.employee().getMbti()));
        }

        messages.add(new ChatMessageDTO("system", "Trend summary:\n"
                + "WORK: " + data.trendSummary().workTrend() + "\n"
                + "EMOTION: " + data.trendSummary().emotionTrend() + "\n"
                + "TASK: " + data.trendSummary().taskTrend()));

        if (!data.memories().isEmpty()) {
            messages.add(new ChatMessageDTO("system", "Agent memory (latest):\n" + formatMemories(data.memories())));
        }

        messages.add(new ChatMessageDTO("system", "Logs (last 3 weeks):\n" + formatLogsAsText(data.logs())));
        messages.add(new ChatMessageDTO("system", "Tasks (last 3 weeks):\n" + formatTasksAsText(data.tasks())));
        messages.add(new ChatMessageDTO("system", "Projects:\n" + formatProjectsAsText(data.projects())));

        if (!data.attachmentMetas().isEmpty()) {
            messages.add(new ChatMessageDTO("system",
                    "Attachment metas:\n" + formatAttachmentMetas(data.attachmentMetas())));
        }

        if (!data.attachmentInsights().isEmpty()) {
            messages.add(new ChatMessageDTO("system",
                    "Attachment insights:\n" + formatAttachmentInsights(data.attachmentInsights())));
        }

        return messages.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<Log> safeLoadLogs(final Integer userId, final LocalDateTime start, final LocalDateTime end) {
        try {
            log.info("[agent-plan] loading logs userId={} start={} end={}", userId, start, end);
            final List<Log> result = logRepository.findByEmployeeEmployeeIdAndCreatedTimeBetween(userId, start, end);
            log.info("[agent-plan] loaded logs userId={} count={}", userId, result.size());

            if (result.isEmpty()) {
                try {
                    final Object rawCount = entityManager
                            .createNativeQuery(
                                    "SELECT COUNT(*) FROM `log` WHERE employee_id = ?1 AND created_time BETWEEN ?2 AND ?3")
                            .setParameter(1, userId)
                            .setParameter(2, start)
                            .setParameter(3, end)
                            .getSingleResult();

                    final String countText = rawCount == null ? "null" : rawCount.toString();

                    final List<?> latestRows = entityManager
                            .createNativeQuery(
                                    "SELECT log_id, created_time, emoji FROM `log` WHERE employee_id = ?1 ORDER BY created_time DESC LIMIT 3")
                            .setParameter(1, userId)
                            .getResultList();

                    log.info("[agent-plan] native log count userId={} count={} latestRows={}", userId, countText,
                            latestRows);
                } catch (Exception nativeEx) {
                    log.warn("[agent-plan] native log query failed userId={}", userId, nativeEx);
                }
            }

            return result;
        } catch (Exception e) {
            log.warn("Load logs failed", e);
            return List.of();
        }
    }

    private List<Task> safeLoadTasks(final Integer userId, final LocalDateTime since) {
        try {
            final List<Task> assigned = taskRepository.findByAssigneeEmployeeId(userId);
            final List<Task> sent = taskRepository.findBySenderEmployeeId(userId);

            final List<Task> merged = new ArrayList<>();
            merged.addAll(assigned);
            merged.addAll(sent);

            return merged.stream()
                    .distinct()
                    .filter(task -> {
                        final LocalDateTime startTime = task.getStartTime();
                        final LocalDateTime endTime = task.getEndTime();
                        return (startTime != null && startTime.isAfter(since))
                                || (endTime != null && endTime.isAfter(since));
                    })
                    .limit(TASK_LIMIT)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Load tasks failed", e);
            return List.of();
        }
    }

    private List<Project> safeLoadProjects(final Employee employee, final Integer userId) {
        try {
            if (employee == null || employee.getPosition() == null) {
                return List.of();
            }

            // CEO(0) / 部门经理(1): 自己创建的项目
            if (employee.getPosition() <= 1) {
                return projectRepository.findBySenderEmployeeId(userId);
            }

            // 团队长(2) / 员工(3): 所在团队负责的项目
            return projectRepository.findProjectsByEmployeeId(userId);
        } catch (Exception e) {
            log.warn("Load projects failed", e);
            return List.of();
        }
    }

    private List<AttachmentMeta> loadAttachmentMetas(
            final Integer userId,
            final List<Log> logs,
            final List<Task> tasks) {
        final List<AttachmentMeta> result = new ArrayList<>();

        for (Log logItem : logs) {
            result.addAll(attachmentInsightService.listLogAttachments(userId, logItem.getLogId()));
        }

        for (Task task : tasks) {
            result.addAll(attachmentInsightService.listTaskAttachments(userId, task.getTaskId()));
        }

        return result;
    }

    private List<AttachmentInsight> decideAndAnalyzeAttachments(
            final List<Log> logs,
            final List<Task> tasks,
            final List<AttachmentMeta> metas) {
        if (metas.isEmpty()) {
            return List.of();
        }

        final boolean shouldRead = shouldReadAttachments(logs, tasks);
        if (!shouldRead) {
            return List.of();
        }

        return attachmentInsightService.maybeAnalyzeAttachments(metas);
    }

    private boolean shouldReadAttachments(final List<Log> logs, final List<Task> tasks) {
        for (Log logItem : logs) {
            if (logItem.getContent() != null && logItem.getContent().contains("附件")) {
                return true;
            }
        }

        for (Task task : tasks) {
            final Byte priority = task.getTaskPriority();
            if (priority != null && priority >= 2) {
                return true;
            }
        }
        return false;
    }

    private void upsertMemories(final Integer userId, final WorkReportTrendSummary summary) {
        agentMemoryService.appendMemory(userId, AgentMemoryType.WORK_TREND, summary.workTrend());
        agentMemoryService.appendMemory(userId, AgentMemoryType.EMOTION_TREND, summary.emotionTrend());
        agentMemoryService.appendMemory(userId, AgentMemoryType.TASK_TREND, summary.taskTrend());
    }

    private List<ChatMessageDTO> buildPrompt(
            final Employee employee,
            final List<Log> logs,
            final List<Task> tasks,
            final List<AttachmentMeta> attachmentMetas,
            final List<AttachmentInsight> attachmentInsights,
            final List<Project> projects,
            final List<AgentMemory> memories,
            final WorkReportTrendSummary trendSummary) {
        final List<ChatMessageDTO> messages = new ArrayList<>();

        messages.add(new ChatMessageDTO(
                "system",
                "你是一个智能工作助手，专门帮助用户分析工作日志和任务数据，提供个性化的工作建议。你必须使用中文回复。"));

        messages.add(new ChatMessageDTO(
                "system",
                "你的回答必须严格按照以下三个主题结构组织，每个主题用纯文本标题开头（不使用emoji）：\n\n"
                        + "【工作节奏建议】\n"
                        + "要求：不少于50字，至少包含两句话。\n\n"
                        + "【情绪健康提醒】\n"
                        + "要求：不少于50字，至少包含两句话，必须基于日志中的实际心情数据。\n\n"
                        + "【任务完成趋势】\n"
                        + "要求：不少于50字，至少包含两句话。\n\n"
                        + "如果某个主题缺少数据支撑，明确说明。如果你收到了附件解析,请先分析附件解析结果,再试着使用它配合任务完成趋势分析。\n"
                        + "你必须使用中文回复。\n"
                        + "不要编造不存在的日志、任务、项目或附件内容；涉及数量/比例时仅使用上下文中明确给出的数据，否则说明无法从数据推断。\n"
                        + "不要输出或猜测任何敏感信息（如密码、验证码、密钥、完整手机号等）。"));

        if (employee != null && employee.getMbti() != null && !employee.getMbti().isEmpty()) {
            messages.add(new ChatMessageDTO("system", "用户的MBTI性格类型是：" + employee.getMbti()));
        }

        messages.add(new ChatMessageDTO("system", "Long-term memory (trend summary):\n"
                + "WORK: " + trendSummary.workTrend() + "\n"
                + "EMOTION: " + trendSummary.emotionTrend() + "\n"
                + "TASK: " + trendSummary.taskTrend()));

        if (!memories.isEmpty()) {
            messages.add(
                    new ChatMessageDTO("system", "Long-term memory (latest entries):\n" + formatMemories(memories)));
        }

        messages.add(new ChatMessageDTO("system", "以下是用户近三周的工作日志（共 " + logs.size() + " 条）：\n"
                + formatLogsAsText(logs)));

        messages.add(new ChatMessageDTO("system", "以下是用户近三周相关的任务（共 " + tasks.size() + " 个）：\n"
                + formatTasksAsText(tasks)));

        messages.add(new ChatMessageDTO("system", "以下是用户相关的项目（共 " + projects.size() + " 个）：\n"
                + formatProjectsAsText(projects)));

        messages.add(new ChatMessageDTO("system", "附件元信息（共 " + attachmentMetas.size() + " 个）：\n"
                + formatAttachmentMetas(attachmentMetas)));

        if (!attachmentInsights.isEmpty()) {
            messages.add(new ChatMessageDTO("system", "已解析附件摘要（共 " + attachmentInsights.size() + " 个）：\n"
                    + formatAttachmentInsights(attachmentInsights)));
        }

        messages.add(new ChatMessageDTO(
                "user",
                "请基于我近三周的工作日志、任务、以及可用的附件摘要，生成工作分析报告。"));

        return messages;
    }

    private String formatLogsAsText(final List<Log> logs) {
        return logs.stream()
                .sorted((l1, l2) -> l2.getCreatedTime().compareTo(l1.getCreatedTime()))
                .limit(LOG_LIMIT)
                .map(log -> "- " + log.getCreatedTime().format(DATE_FORMATTER)
                        + " " + (log.getEmoji() == null ? "平静" : log.getEmoji().getDesc())
                        + " " + log.getContent()
                        + (log.getTask() != null ? " [关联任务: " + log.getTask().getTitle() + "]" : ""))
                .collect(Collectors.joining("\n"));
    }

    private String formatTasksAsText(final List<Task> tasks) {
        return tasks.stream()
                .limit(TASK_LIMIT)
                .map(task -> "- [status=" + task.getTaskStatus() + "] [priority=" + task.getTaskPriority() + "] "
                        + task.getTitle()
                        + (task.getEndTime() != null ? " (截止: " + task.getEndTime().format(DATE_FORMATTER) + ")" : "")
                        + (task.getContent() != null && !task.getContent().isEmpty() ? " - " + task.getContent() : ""))
                .collect(Collectors.joining("\n"));
    }

    private String formatProjectsAsText(final List<Project> projects) {
        if (projects.isEmpty()) {
            return "(none)";
        }

        return projects.stream()
                .limit(10)
                .map(project -> "- [status=" + project.getProjectStatus() + "] "
                        + project.getTitle()
                        + (project.getContent() != null && !project.getContent().isEmpty()
                                ? " - " + project.getContent()
                                : "")
                        + (project.getEndTime() != null ? " (end: " + project.getEndTime().format(DATE_FORMATTER) + ")"
                                : ""))
                .collect(Collectors.joining("\n"));
    }

    private String formatMemories(final List<AgentMemory> memories) {
        if (memories.isEmpty()) {
            return "(none)";
        }

        return memories.stream()
                .limit(10)
                .map(memory -> "- [" + memory.getMemoryType() + "] " + memory.getContent())
                .collect(Collectors.joining("\n"));
    }

    private String formatAttachmentMetas(final List<AttachmentMeta> metas) {
        if (metas.isEmpty()) {
            return "(none)";
        }
        return metas.stream()
                .limit(20)
                .map(meta -> "- [id=" + meta.attachmentId() + "] " + meta.originalFilename()
                        + " type=" + meta.fileType() + " size=" + meta.fileSize())
                .collect(Collectors.joining("\n"));
    }

    private String formatAttachmentInsights(final List<AttachmentInsight> insights) {
        return insights.stream()
                .limit(5)
                .map(insight -> {
                    final String status = insight.error() == null ? "OK" : "FAILED";
                    final String summary = insight.summary() == null ? "" : insight.summary();
                    final String error = insight.error() == null ? "" : insight.error();
                    return "- [" + status + "] " + insight.originalFilename() + ": " + summary
                            + (error.isEmpty() ? "" : " (" + error + ")");
                })
                .collect(Collectors.joining("\n"));
    }

    private void chatStreamAndSave(
            final List<ChatMessageDTO> messages,
            final SseEmitter emitter,
            final Integer userId,
            final LocalDateTime periodStart,
            final LocalDateTime periodEnd,
            final int logCount,
            final int taskCount) {
        final StringBuilder fullContent = new StringBuilder();

        new Thread(() -> {
            try {
                ChatRequestDTO request = new ChatRequestDTO();
                request.setMessages(messages);
                request.setModel(glmConfig.getModel());
                request.setStream(true);

                String requestBody = objectMapper.writeValueAsString(request);

                URL url = new URI(glmConfig.getApiUrl() + "/chat/completions").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + glmConfig.getApiKey());
                conn.setDoOutput(true);
                conn.setConnectTimeout(glmConfig.getTimeout() * 1000);
                conn.setReadTimeout(glmConfig.getTimeout() * 1000);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    emitter.completeWithError(new RuntimeException("GLM API error code: " + responseCode));
                    return;
                }

                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (!line.startsWith("data: ")) {
                            continue;
                        }

                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) {
                            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                            emitter.complete();
                            saveAiAnalysis(userId, fullContent.toString(), periodStart, periodEnd, logCount, taskCount);
                            return;
                        }

                        try {
                            JsonNode jsonData = objectMapper.readTree(data);
                            JsonNode choices = jsonData.get("choices");
                            if (choices == null || choices.isEmpty()) {
                                continue;
                            }

                            JsonNode delta = choices.get(0).get("delta");
                            if (delta != null && delta.has("content")) {
                                String content = delta.get("content").asText();
                                fullContent.append(content);
                                emitter.send(SseEmitter.event().name("message").data(content));
                            }
                        } catch (Exception ignore) {
                        }
                    }
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        }).start();
    }

    private void saveAiAnalysis(
            final Integer userId,
            final String fullContent,
            final LocalDateTime periodStart,
            final LocalDateTime periodEnd,
            final int logCount,
            final int taskCount) {
        try {
            final AiAnalysis analysis = new AiAnalysis();

            final Employee employee = new Employee();
            employee.setEmployeeId(userId);
            analysis.setEmployee(employee);

            analysis.setCreatedTime(LocalDateTime.now());
            analysis.setPeriodStart(periodStart);
            analysis.setPeriodEnd(periodEnd);
            analysis.setFullContent(fullContent);
            analysis.setLogCount(logCount);
            analysis.setTaskCount(taskCount);

            parseAndSetThemes(analysis, fullContent);
            aiAnalysisRepository.save(analysis);
        } catch (Exception e) {
            log.warn("Save ai_analysis failed", e);
        }
    }

    private void parseAndSetThemes(final AiAnalysis analysis, final String fullContent) {
        try {
            int rhythmStart = fullContent.indexOf("【工作节奏建议】");
            int rhythmEnd = fullContent.indexOf("【情绪健康提醒】");
            if (rhythmStart >= 0 && rhythmEnd > rhythmStart) {
                analysis.setWorkRhythmAdvice(fullContent.substring(rhythmStart, rhythmEnd).trim());
            }

            int emotionStart = fullContent.indexOf("【情绪健康提醒】");
            int emotionEnd = fullContent.indexOf("【任务完成趋势】");
            if (emotionStart >= 0 && emotionEnd > emotionStart) {
                analysis.setEmotionHealthReminder(fullContent.substring(emotionStart, emotionEnd).trim());
            }

            int taskStart = fullContent.indexOf("【任务完成趋势】");
            if (taskStart >= 0) {
                analysis.setTaskCompletionTrend(fullContent.substring(taskStart).trim());
            }
        } catch (Exception ignore) {
        }
    }
}
