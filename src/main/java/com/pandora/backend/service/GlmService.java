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
 * Service for GLM-4.6 API integration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GlmService {
    
    private final GlmConfig glmConfig;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    /**
     * Non-streaming chat completion.
     *
     * @param messages List of chat messages
     * @return ChatResponseDTO with complete response
     */
    public ChatResponseDTO chat(List<ChatMessageDTO> messages) {
        try {
            ChatRequestDTO request = new ChatRequestDTO();
            request.setMessages(messages);
            request.setModel(glmConfig.getModel());
            request.setStream(false);
            
            String requestBody = objectMapper.writeValueAsString(request);
            log.info("Sending non-streaming request to GLM API");
            
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
                    
                    log.info("Received complete response from GLM API");
                    
                    return ChatResponseDTO.builder()
                            .content(content)
                            .model(glmConfig.getModel())
                            .finishReason(finishReason)
                            .totalTokens(totalTokens)
                            .build();
                }
            } else {
                log.error("GLM API error: {}", responseCode);
                throw new RuntimeException("GLM API returned error code: " + responseCode);
            }
            
        } catch (Exception e) {
            log.error("Error calling GLM API", e);
            throw new RuntimeException("Failed to call GLM API", e);
        }
    }
    
    /**
     * Streaming chat completion using SSE.
     *
     * @param messages List of chat messages
     * @param emitter SSE emitter for streaming response
     */
    public void chatStream(List<ChatMessageDTO> messages, SseEmitter emitter) {
        executorService.execute(() -> {
            try {
                ChatRequestDTO request = new ChatRequestDTO();
                request.setMessages(messages);
                request.setModel(glmConfig.getModel());
                request.setStream(true);
                
                String requestBody = objectMapper.writeValueAsString(request);
                log.info("Sending streaming request to GLM API");
                
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
                                    log.info("Stream completed");
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
                                    log.warn("Error parsing stream data: {}", data, e);
                                }
                            }
                        }
                    }
                } else {
                    log.error("GLM API error: {}", responseCode);
                    emitter.completeWithError(
                            new RuntimeException("GLM API returned error code: " + responseCode));
                }
                
            } catch (Exception e) {
                log.error("Error in streaming chat", e);
                emitter.completeWithError(e);
            }
        });
    }
}
