package com.pandora.backend.service;

import com.pandora.backend.config.GlmConfig;
import com.pandora.backend.dto.ChatMessageDTO;
import com.pandora.backend.dto.ChatRequestDTO;
import com.pandora.backend.dto.ChatResponseDTO;
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
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
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
     * @param emitter 用于流式响应的 SSE 发射器
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
}
