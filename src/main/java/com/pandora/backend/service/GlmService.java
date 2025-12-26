package com.pandora.backend.service;

import com.pandora.backend.config.GlmConfig;
import com.pandora.backend.dto.ChatMessageDTO;
import com.pandora.backend.dto.ChatRequestDTO;
import com.pandora.backend.dto.ChatResponseDTO;
import com.pandora.backend.entity.AiAnalysis;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.Log;
import com.pandora.backend.entity.Task;
import com.pandora.backend.repository.AiAnalysisRepository;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.repository.LogRepository;
import com.pandora.backend.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * GLM-4.6 API 集成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlmService {

    private final GlmConfig glmConfig;
    private final ObjectMapper objectMapper;
    private final LogRepository logRepository;
    private final TaskRepository taskRepository;
    private final AiAnalysisRepository aiAnalysisRepository;
    private final EmployeeRepository employeeRepository;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 非流式聊天完成
     *
     * @param messages 聊天消息列表
     * @return 包含完整响应的 ChatResponseDTO
     */
    public ChatResponseDTO chat(List<ChatMessageDTO> messages) {
        try {
            ChatRequestDTO request = new ChatRequestDTO();
            request.setMessages(messages);
            request.setModel(glmConfig.getModel());
            request.setStream(false);

            String requestBody = objectMapper.writeValueAsString(request);
            log.info("发送非流式请求到 GLM API");

            URL url = new URL(glmConfig.getApiUrl() + "/chat/completions");
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
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }

                    JsonNode jsonResponse = objectMapper.readTree(response.toString());
                    JsonNode choice = jsonResponse.get("choices").get(0);
                    String content = choice.get("message").get("content").asText();
                    String finishReason = choice.get("finish_reason").asText();

                    JsonNode usage = jsonResponse.get("usage");
                    int totalTokens = usage.get("total_tokens").asInt();

                    log.info("从 GLM API 收到完整响应");

                    return ChatResponseDTO.builder()
                            .content(content)
                            .model(glmConfig.getModel())
                            .finishReason(finishReason)
                            .totalTokens(totalTokens)
                            .build();
                }
            } else {
                log.error("GLM API 错误: {}", responseCode);
                throw new RuntimeException("GLM API 返回错误代码: " + responseCode);
            }

        } catch (Exception e) {
            log.error("调用 GLM API 错误", e);
            throw new RuntimeException("调用 GLM API 失败", e);
        }
    }

    /**
     * 使用 SSE 的流式聊天完成
     *
     * @param messages 聊天消息列表
     * @param emitter  用于流式响应的 SSE 发射器
     */
    public void chatStream(List<ChatMessageDTO> messages, SseEmitter emitter) {
        executorService.execute(() -> {
            try {
                ChatRequestDTO request = new ChatRequestDTO();
                request.setMessages(messages);
                request.setModel(glmConfig.getModel());
                request.setStream(true);

                String requestBody = objectMapper.writeValueAsString(request);
                log.info("发送流式请求到 GLM API");

                URL url = new URL(glmConfig.getApiUrl() + "/chat/completions");
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
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6);

                                if ("[DONE]".equals(data)) {
                                    log.info("流式响应完成");
                                    emitter.send(SseEmitter.event()
                                            .name("done")
                                            .data("[DONE]"));
                                    emitter.complete();
                                    break;
                                }

                                try {
                                    JsonNode jsonData = objectMapper.readTree(data);
                                    JsonNode choices = jsonData.get("choices");

                                    if (choices != null && choices.size() > 0) {
                                        JsonNode delta = choices.get(0).get("delta");
                                        if (delta != null && delta.has("content")) {
                                            String content = delta.get("content").asText();

                                            emitter.send(SseEmitter.event()
                                                    .name("message")
                                                    .data(content));

                                            log.debug("Sent chunk: {}", content);
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("解析流式数据错误: {}", data, e);
                                }
                            }
                        }
                    }
                } else {
                    log.error("GLM API 错误: {}", responseCode);
                    emitter.completeWithError(
                            new RuntimeException("GLM API 返回错误代码: " + responseCode));
                }

            } catch (Exception e) {
                log.error("流式聊天错误", e);
                emitter.completeWithError(e);
            }
        });
    }

    /**
     * 带上下文的流式聊天（包含固定提示词 + 日志任务上下文 + 用户消息）
     * 并在完成后保存分析结果到数据库
     *
     * @param userId       用户ID
     * @param userMessages 用户原始消息列表
     * @param emitter      SSE 发射器
     */
    public void chatStreamWithContext(
            final Integer userId,
            final List<ChatMessageDTO> userMessages,
            final SseEmitter emitter) {
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime threeWeeksAgo = now.minusWeeks(3);

        // 1. 构建固定提示词
        final List<ChatMessageDTO> systemPrompts = buildFixedSystemPrompts();

        // 2. 构建日志和任务上下文
        final List<ChatMessageDTO> contextMessages = buildContextMessagesForUser(userId, now);

        // 3. 合并所有消息：系统提示 + 上下文 + 用户消息
        final List<ChatMessageDTO> allMessages = new ArrayList<>();
        allMessages.addAll(systemPrompts);
        allMessages.addAll(contextMessages);
        allMessages.addAll(userMessages);

        log.info("为用户 {} 构建了完整上下文，共 {} 条消息（系统提示: {}, 上下文: {}, 用户消息: {}）",
                userId, allMessages.size(), systemPrompts.size(), contextMessages.size(), userMessages.size());

        // 4. 调用带保存功能的流式聊天方法
        chatStreamAndSave(allMessages, emitter, userId, threeWeeksAgo, now);
    }

    /**
     * 流式聊天并保存结果到数据库
     *
     * @param messages    完整消息列表
     * @param emitter     SSE 发射器
     * @param userId      用户ID
     * @param periodStart 分析周期开始时间
     * @param periodEnd   分析周期结束时间
     */
    private void chatStreamAndSave(
            final List<ChatMessageDTO> messages,
            final SseEmitter emitter,
            final Integer userId,
            final LocalDateTime periodStart,
            final LocalDateTime periodEnd) {

        final StringBuilder fullContent = new StringBuilder();
        final int[] logCount = { 0 };
        final int[] taskCount = { 0 };

        // 从上下文消息中提取日志和任务数量
        for (ChatMessageDTO msg : messages) {
            if (msg.getRole().equals("system") && msg.getContent().contains("共")) {
                if (msg.getContent().contains("日志")) {
                    try {
                        String content = msg.getContent();
                        int start = content.indexOf("共 ") + 2;
                        int end = content.indexOf(" 条", start);
                        logCount[0] = Integer.parseInt(content.substring(start, end));
                    } catch (Exception e) {
                        log.warn("解析日志数量失败", e);
                    }
                }
                if (msg.getContent().contains("任务")) {
                    try {
                        String content = msg.getContent();
                        int start = content.indexOf("共 ") + 2;
                        int end = content.indexOf(" 个", start);
                        taskCount[0] = Integer.parseInt(content.substring(start, end));
                    } catch (Exception e) {
                        log.warn("解析任务数量失败", e);
                    }
                }
            }
        }

        executorService.execute(() -> {
            try {
                ChatRequestDTO request = new ChatRequestDTO();
                request.setMessages(messages);
                request.setModel(glmConfig.getModel());
                request.setStream(true);

                String requestBody = objectMapper.writeValueAsString(request);
                log.info("发送流式请求到 GLM API");

                URL url = new URL(glmConfig.getApiUrl() + "/chat/completions");
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
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6);

                                if ("[DONE]".equals(data)) {
                                    log.info("流式响应完成");
                                    emitter.send(SseEmitter.event()
                                            .name("done")
                                            .data("[DONE]"));
                                    emitter.complete();

                                    // 保存完整内容到数据库
                                    saveAiAnalysis(userId, fullContent.toString(), periodStart, periodEnd,
                                            logCount[0], taskCount[0]);
                                    break;
                                }

                                try {
                                    JsonNode jsonData = objectMapper.readTree(data);
                                    JsonNode choices = jsonData.get("choices");

                                    if (choices != null && choices.size() > 0) {
                                        JsonNode delta = choices.get(0).get("delta");
                                        if (delta != null && delta.has("content")) {
                                            String content = delta.get("content").asText();

                                            // 收集完整内容
                                            fullContent.append(content);

                                            emitter.send(SseEmitter.event()
                                                    .name("message")
                                                    .data(content));

                                            log.debug("Sent chunk: {}", content);
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("解析流式数据错误: {}", data, e);
                                }
                            }
                        }
                    }
                } else {
                    log.error("GLM API 错误: {}", responseCode);
                    emitter.completeWithError(
                            new RuntimeException("GLM API 返回错误代码: " + responseCode));
                }

            } catch (Exception e) {
                log.error("流式聊天错误", e);
                emitter.completeWithError(e);
            }
        });
    }

    /**
     * 保存 AI 分析结果到数据库
     */
    private void saveAiAnalysis(
            final Integer userId,
            final String fullContent,
            final LocalDateTime periodStart,
            final LocalDateTime periodEnd,
            final int logCount,
            final int taskCount) {
        try {
            // 创建新的分析记录
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

            // 尝试解析三个主题的内容
            parseAndSetThemes(analysis, fullContent);

            aiAnalysisRepository.save(analysis);
            log.info("已为用户 {} 保存 AI 分析结果到数据库", userId);
        } catch (Exception e) {
            log.error("保存 AI 分析结果失败", e);
        }
    }

    /**
     * 从完整内容中解析三个主题
     */
    private void parseAndSetThemes(final AiAnalysis analysis, final String fullContent) {
        try {
            // 解析工作节奏建议
            int rhythmStart = fullContent.indexOf("【工作节奏建议】");
            int rhythmEnd = fullContent.indexOf("【情绪健康提醒】");
            if (rhythmStart >= 0 && rhythmEnd > rhythmStart) {
                String rhythm = fullContent.substring(rhythmStart, rhythmEnd).trim();
                analysis.setWorkRhythmAdvice(rhythm);
            }

            // 解析情绪健康提醒
            int emotionStart = fullContent.indexOf("【情绪健康提醒】");
            int emotionEnd = fullContent.indexOf("【任务完成趋势】");
            if (emotionStart >= 0 && emotionEnd > emotionStart) {
                String emotion = fullContent.substring(emotionStart, emotionEnd).trim();
                analysis.setEmotionHealthReminder(emotion);
            }

            // 解析任务完成趋势
            int taskStart = fullContent.indexOf("【任务完成趋势】");
            if (taskStart >= 0) {
                String task = fullContent.substring(taskStart).trim();
                analysis.setTaskCompletionTrend(task);
            }
        } catch (Exception e) {
            log.warn("解析主题内容失败，将只保存完整内容", e);
        }
    }

    /**
     * 构建固定的系统提示词（包含三主题要求）
     *
     * @return 系统提示词消息列表
     */
    private List<ChatMessageDTO> buildFixedSystemPrompts() {
        final List<ChatMessageDTO> prompts = new ArrayList<>();

        // 1. 角色和身份设定
        prompts.add(new ChatMessageDTO(
                "system",
                "你是一个智能工作助手，专门帮助用户分析工作日志和任务数据，提供个性化的工作建议。" +
                        "你需要基于用户的日志记录（包含时间、内容、心情emoji）和任务信息（包含标题、内容、优先级、状态、截止时间）来进行分析。"));

        // 2. 输出格式要求（三主题结构 + 字数限制）
        prompts.add(new ChatMessageDTO(
                "system",
                "你的回答必须严格按照以下三个主题结构组织，每个主题用纯文本标题开头（不使用emoji）：\n\n" +
                        "【工作节奏建议】\n" +
                        "基于用户近期的工作效率、任务完成情况和工作时间分布，给出具体的节奏调整建议。\n" +
                        "要求：不超过50字，至少包含两句话。\n\n" +
                        "【情绪健康提醒】\n" +
                        "务必分析用户日志中的心情emoji（开心😊、压力😰、平静😌、疲惫😴、生气😠）的分布和变化趋势。\n" +
                        "重点关注：压力和负面情绪的频率、持续时间、触发场景。\n" +
                        "如果提供了用户的MBTI性格类型，请结合MBTI特点给出针对性的情绪管理建议。\n" +
                        "要求：不超过50字，至少包含两句话，必须基于日志中的实际心情数据。\n\n" +
                        "【任务完成趋势】\n" +
                        "总结任务完成率、优先级分布、延期情况等关键指标，指出需要关注的任务。\n" +
                        "要求：不超过50字，至少包含两句话。\n\n" +
                        "每个主题的内容要简洁、具体、可操作，避免空泛的建议。如果某个主题缺少数据支撑，明确说明。"));

        // 3. 分析原则
        prompts.add(new ChatMessageDTO(
                "system",
                "分析原则：\n" +
                        "1. 优先使用提供的日志和任务数据，不要编造不存在的信息\n" +
                        "2. 如果数据不足以支撑某个结论，明确告知用户\n" +
                        "3. 建议要具体可行，避免泛泛而谈\n" +
                        "4. 用中文回答，语气友好专业\n" +
                        "5. 关注用户的工作健康和可持续发展"));

        return prompts;
    }

    /**
     * 根据用户ID和当前时间筛选近三周的日志和任务，并组装成上下文消息
     *
     * @param userId 用户ID
     * @param now    当前时间
     * @return 上下文消息列表
     */
    private List<ChatMessageDTO> buildContextMessagesForUser(
            final Integer userId,
            final LocalDateTime now) {
        final List<ChatMessageDTO> contextMessages = new ArrayList<>();

        // 计算三周前的时间点
        final LocalDateTime threeWeeksAgo = now.minusWeeks(3);

        try {
            // 0. 查询员工的 MBTI 信息
            final Employee employee = employeeRepository.findById(userId).orElse(null);
            if (employee != null && employee.getMbti() != null && !employee.getMbti().isEmpty()) {
                contextMessages.add(new ChatMessageDTO(
                        "system",
                        "用户的MBTI性格类型是：" + employee.getMbti() +
                                "。请在情绪健康提醒中结合该性格类型的特点给出针对性建议。"));
                log.info("为用户 {} 加载了 MBTI 信息: {}", userId, employee.getMbti());
            }

            // 1. 查询近三周的日志
            final List<Log> recentLogs = logRepository.findByEmployeeEmployeeIdAndCreatedTimeBetween(
                    userId, threeWeeksAgo, now);

            if (!recentLogs.isEmpty()) {
                final String logsSummary = formatLogsAsText(recentLogs);
                contextMessages.add(new ChatMessageDTO(
                        "system",
                        "以下是用户近三周的工作日志（共 " + recentLogs.size() + " 条）：\n" + logsSummary));
                log.info("为用户 {} 加载了 {} 条近三周日志", userId, recentLogs.size());
            } else {
                contextMessages.add(new ChatMessageDTO(
                        "system",
                        "用户近三周没有工作日志记录。"));
                log.info("用户 {} 近三周没有日志记录", userId);
            }

            // 2. 查询近三周相关的任务（作为执行者或发送者）
            final List<Task> assignedTasks = taskRepository.findByAssigneeEmployeeId(userId);
            final List<Task> sentTasks = taskRepository.findBySenderEmployeeId(userId);

            // 合并并去重，筛选近三周有活动的任务（根据开始时间或结束时间）
            final List<Task> recentTasks = new ArrayList<>();
            recentTasks.addAll(assignedTasks);
            recentTasks.addAll(sentTasks);

            final List<Task> filteredTasks = recentTasks.stream()
                    .distinct()
                    .filter(task -> {
                        final LocalDateTime startTime = task.getStartTime();
                        final LocalDateTime endTime = task.getEndTime();
                        // 任务的开始时间或结束时间在近三周内
                        return (startTime != null && startTime.isAfter(threeWeeksAgo)) ||
                                (endTime != null && endTime.isAfter(threeWeeksAgo));
                    })
                    .collect(Collectors.toList());

            if (!filteredTasks.isEmpty()) {
                final String tasksSummary = formatTasksAsText(filteredTasks);
                contextMessages.add(new ChatMessageDTO(
                        "system",
                        "以下是用户近三周相关的任务（共 " + filteredTasks.size() + " 个）：\n" + tasksSummary));
                log.info("为用户 {} 加载了 {} 个近三周任务", userId, filteredTasks.size());
            } else {
                contextMessages.add(new ChatMessageDTO(
                        "system",
                        "用户近三周没有相关任务记录。"));
                log.info("用户 {} 近三周没有任务记录", userId);
            }

        } catch (Exception e) {
            log.error("为用户 {} 构建上下文时出错", userId, e);
            contextMessages.add(new ChatMessageDTO(
                    "system",
                    "获取用户日志和任务数据时出现错误，将基于用户提问进行回答。"));
        }

        return contextMessages;
    }

    /**
     * 将日志列表格式化为文本
     *
     * @param logs 日志列表
     * @return 格式化后的文本
     */
    private String formatLogsAsText(final List<Log> logs) {
        final StringBuilder sb = new StringBuilder();

        // 按时间倒序排列（最新的在前）
        logs.stream()
                .sorted((l1, l2) -> l2.getCreatedTime().compareTo(l1.getCreatedTime()))
                .limit(50) // 最多取50条，避免上下文过长
                .forEach(log -> {
                    sb.append("- ");
                    sb.append(log.getCreatedTime().format(DATE_FORMATTER));
                    sb.append(" ");
                    sb.append(getEmojiText(log.getEmoji()));
                    sb.append(" ");
                    sb.append(log.getContent());
                    if (log.getTask() != null) {
                        sb.append(" [关联任务: ").append(log.getTask().getTitle()).append("]");
                    }
                    sb.append("\n");
                });

        return sb.toString();
    }

    /**
     * 将任务列表格式化为文本
     *
     * @param tasks 任务列表
     * @return 格式化后的文本
     */
    private String formatTasksAsText(final List<Task> tasks) {
        final StringBuilder sb = new StringBuilder();

        // 按优先级和状态排序
        tasks.stream()
                .sorted((t1, t2) -> {
                    // 先按状态排序（进行中 > 未开始 > 已完成）
                    int statusCompare = Integer.compare(t1.getTaskStatus(), t2.getTaskStatus());
                    if (statusCompare != 0)
                        return statusCompare;
                    // 再按优先级降序
                    return Integer.compare(t2.getTaskPriority(), t1.getTaskPriority());
                })
                .limit(30) // 最多取30个任务
                .forEach(task -> {
                    sb.append("- ");
                    sb.append("[").append(getTaskStatusText(task.getTaskStatus())).append("] ");
                    sb.append("[").append(getTaskPriorityText(task.getTaskPriority())).append("] ");
                    sb.append(task.getTitle());
                    if (task.getEndTime() != null) {
                        sb.append(" (截止: ").append(task.getEndTime().format(DATE_FORMATTER)).append(")");
                    }
                    if (task.getContent() != null && !task.getContent().isEmpty()) {
                        sb.append(" - ").append(task.getContent());
                    }
                    sb.append("\n");
                });

        return sb.toString();
    }

    /**
     * 将 Emoji 枚举转换为文本表示（使用中文描述）
     */
    private String getEmojiText(final com.pandora.backend.enums.Emoji emoji) {
        if (emoji == null)
            return "平静";
        return emoji.getDesc();
    }

    /**
     * 将任务状态转换为文本
     */
    private String getTaskStatusText(final Byte status) {
        if (status == null)
            return "未知";
        return switch (status) {
            case 0 -> "未开始";
            case 1 -> "进行中";
            case 2 -> "已完成";
            case 3 -> "已取消";
            default -> "未知";
        };
    }

    /**
     * 将任务优先级转换为文本
     */
    private String getTaskPriorityText(final Byte priority) {
        if (priority == null)
            return "普通";
        return switch (priority) {
            case 0 -> "低";
            case 1 -> "普通";
            case 2 -> "高";
            case 3 -> "紧急";
            default -> "普通";
        };
    }

    /**
     * 生成 AI 工作分析（流式响应）
     * 基于员工近三周的日志和任务数据，生成任务完成趋势、工作节奏建议和情绪健康提醒
     *
     * @param userId  员工ID
     * @param emitter SSE 发射器
     */
    public void generateAiAnalysis(final Integer userId, final SseEmitter emitter) {
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime threeWeeksAgo = now.minusWeeks(3);

        // 构建固定提示词
        final List<ChatMessageDTO> systemPrompts = buildFixedSystemPrompts();

        // 构建日志和任务上下文
        final List<ChatMessageDTO> contextMessages = buildContextMessagesForUser(userId, now);

        // 添加分析请求消息
        final List<ChatMessageDTO> allMessages = new ArrayList<>();
        allMessages.addAll(systemPrompts);
        allMessages.addAll(contextMessages);
        allMessages.add(new ChatMessageDTO(
                "user",
                "请基于我近三周的工作日志和任务数据，生成工作分析报告。" +
                        "严格按照【工作节奏建议】、【情绪健康提醒】、【任务完成趋势】三个主题输出，每个主题不超过50字。"));

        log.info("为员工 {} 构建了完整的 AI 分析上下文，共 {} 条消息",
                userId, allMessages.size());

        // 调用带保存功能的流式聊天方法
        chatStreamAndSave(allMessages, emitter, userId, threeWeeksAgo, now);
    }
}
