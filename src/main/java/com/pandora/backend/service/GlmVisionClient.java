package com.pandora.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandora.backend.config.GlmConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlmVisionClient {

    private final GlmConfig glmConfig;
    private final ObjectMapper objectMapper;

    public String analyzeImageUrl(final String imageUrl, final String promptText) {
        return analyzeAttachmentUrl(imageUrl, "image/*", promptText);
    }

    public String analyzeAttachmentUrl(final String url, final String fileType, final String promptText) {
        try {
            final String requestBody = buildRequestBody(url, fileType, promptText);

            URL urlObj = new URI(glmConfig.getApiUrl() + "/chat/completions").toURL();
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();
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
                    throw new RuntimeException("GLM vision API error code: " + responseCode);
                }
                throw new RuntimeException("GLM vision API error code: " + responseCode + ", body: " + errorBody);
            }

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
                JsonNode jsonResponse = objectMapper.readTree(response.toString());
                return jsonResponse.get("choices").get(0).get("message").get("content").asText();
            }
        } catch (Exception e) {
            log.error("Vision analyze failed", e);
            throw new RuntimeException("Vision analyze failed", e);
        }
    }

    private String buildRequestBody(final String attachmentUrl, final String fileType, final String promptText) {
        final com.fasterxml.jackson.databind.node.ObjectNode root = objectMapper.createObjectNode();
        root.put("model", glmConfig.getVisionModel());

        final com.fasterxml.jackson.databind.node.ArrayNode content = objectMapper.createArrayNode();
        if (fileType != null && fileType.startsWith("image/")) {
            content.add(objectMapper.createObjectNode()
                    .put("type", "image_url")
                    .set("image_url", objectMapper.createObjectNode().put("url", attachmentUrl)));
        } else {
            content.add(objectMapper.createObjectNode()
                    .put("type", "file_url")
                    .set("file_url", objectMapper.createObjectNode().put("url", attachmentUrl)));
        }
        content.add(objectMapper.createObjectNode()
                .put("type", "text")
                .put("text", promptText));

        final com.fasterxml.jackson.databind.node.ArrayNode messages = objectMapper.createArrayNode();
        messages.add(objectMapper.createObjectNode()
                .put("role", "user")
                .set("content", content));
        root.set("messages", messages);

        root.set("thinking", objectMapper.createObjectNode().put("type", "enabled"));
        return root.toString();
    }
}
