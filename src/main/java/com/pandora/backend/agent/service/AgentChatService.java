package com.pandora.backend.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandora.backend.config.GlmConfig;
import com.pandora.backend.dto.AgentChatStreamRequestDTO;
import com.pandora.backend.dto.ChatMessageDTO;
import com.pandora.backend.dto.ChatRequestDTO;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentChatService {

    private static final long SSE_TIMEOUT_MS = 90000L;

    private final WorkReportAgentService workReportAgentService;
    private final AgentChatSessionService agentChatSessionService;
    private final GlmConfig glmConfig;
    private final ObjectMapper objectMapper;

    private final ExecutorService executorService = Executors.newCachedThreadPool();

    public SseEmitter chatStream(final Integer userId, final AgentChatStreamRequestDTO request) {
        final SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        final String sessionId = resolveSessionId(request.getSessionId());

        emitter.onCompletion(() -> log.info("[agent-chat] sse completed userId={} sessionId={}", userId, sessionId));
        emitter.onTimeout(() -> {
            log.warn("[agent-chat] sse timeout userId={} sessionId={}", userId, sessionId);
            emitter.complete();
        });
        emitter.onError(ex -> {
            log.warn("[agent-chat] sse error userId={} sessionId={}", userId, sessionId, ex);
            emitter.completeWithError(ex);
        });

        executorService.execute(() -> doChatStream(userId, sessionId, request, emitter));
        return emitter;
    }

    private void doChatStream(
            final Integer userId,
            final String sessionId,
            final AgentChatStreamRequestDTO request,
            final SseEmitter emitter) {

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            emitter.completeWithError(new IllegalArgumentException("message is blank"));
            return;
        }

        final int weeks = request.getWeeks() == null ? 3 : request.getWeeks();
        final boolean includeAttachments = request.getIncludeAttachments() == null ? true
                : request.getIncludeAttachments();

        final List<ChatMessageDTO> contextMessages;
        try {
            contextMessages = workReportAgentService.buildChatContextMessages(userId, weeks, includeAttachments);
        } catch (Exception e) {
            emitter.completeWithError(e);
            return;
        }

        final List<ChatMessageDTO> history = agentChatSessionService.getHistory(userId, sessionId);
        final ChatMessageDTO userMessage = new ChatMessageDTO("user", request.getMessage());

        final List<ChatMessageDTO> allMessages = new ArrayList<>();
        allMessages.add(new ChatMessageDTO(
                "system",
                "你是一个智能工作助手。你必须使用中文回复。\n"
                        + "请基于后续提供的上下文回答用户问题；如果上下文缺失或不足以支持结论，明确说明无法判断，并告诉用户需要补充哪些信息。\n"
                        + "不要编造不存在的日志、任务、项目、附件内容或数据；涉及数量/比例时仅使用上下文中明确给出的数据。\n"
                        + "不要输出或猜测任何敏感信息（如密码、验证码、密钥、完整手机号等）。"));
        allMessages.addAll(contextMessages);
        allMessages.addAll(history);
        allMessages.add(userMessage);

        try {
            emitter.send(SseEmitter.event().name("session").data(sessionId));
        } catch (Exception e) {
            emitter.completeWithError(e);
            return;
        }

        final StringBuilder fullContent = new StringBuilder();
        try {
            ChatRequestDTO glmRequest = new ChatRequestDTO();
            glmRequest.setMessages(allMessages);
            glmRequest.setModel(glmConfig.getModel());
            glmRequest.setStream(true);

            final String requestBody = objectMapper.writeValueAsString(glmRequest);

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
                final String errorBody;
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    errorBody = response.toString();
                } catch (Exception ignore) {
                    emitter.completeWithError(new RuntimeException("GLM API error code: " + responseCode));
                    return;
                }
                emitter.completeWithError(
                        new RuntimeException("GLM API error code: " + responseCode + ", body: " + errorBody));
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
                        try {
                            agentChatSessionService.appendTurn(userId, sessionId, userMessage,
                                    new ChatMessageDTO("assistant", fullContent.toString()));
                        } catch (Exception e) {
                            log.warn("[agent-chat] save session failed userId={} sessionId={}", userId, sessionId, e);
                        }

                        emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                        emitter.complete();
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
    }

    private String resolveSessionId(final String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            return sessionId.trim();
        }
        return UUID.randomUUID().toString();
    }
}
