package com.pandora.backend.controller;

import com.pandora.backend.dto.ChatMessageDTO;
import com.pandora.backend.dto.ChatRequestDTO;
import com.pandora.backend.dto.ChatResponseDTO;
import com.pandora.backend.service.GlmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Controller for GLM-4.6 API integration.
 */
@Slf4j
@RestController
@RequestMapping("/api/glm")
@RequiredArgsConstructor
@Tag(name = "GLM API", description = "GLM-4.6 Chat API endpoints")
public class GlmController {
    
    private final GlmService glmService;
    
    /**
     * Non-streaming chat endpoint.
     *
     * @param request Chat request with messages
     * @return Complete chat response
     */
    @PostMapping("/chat")
    @Operation(summary = "Non-streaming chat", description = "Send a chat request and get complete response")
    public ResponseEntity<ChatResponseDTO> chat(@RequestBody ChatRequestDTO request) {
        log.info("Received non-streaming chat request with {} messages", 
                request.getMessages().size());
        
        ChatResponseDTO response = glmService.chat(request.getMessages());
        return ResponseEntity.ok(response);
    }
    
    /**
     * Streaming chat endpoint using SSE.
     *
     * @param request Chat request with messages
     * @return SSE emitter for streaming response
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Streaming chat", description = "Send a chat request and get streaming response via SSE")
    public SseEmitter chatStream(@RequestBody ChatRequestDTO request) {
        log.info("Received streaming chat request with {} messages", 
                request.getMessages().size());
        
        SseEmitter emitter = new SseEmitter(300000L); // 5 minutes timeout
        
        emitter.onCompletion(() -> log.info("SSE completed"));
        emitter.onTimeout(() -> {
            log.warn("SSE timeout");
            emitter.complete();
        });
        emitter.onError((ex) -> {
            log.error("SSE error", ex);
            emitter.completeWithError(ex);
        });
        
        glmService.chatStream(request.getMessages(), emitter);
        
        return emitter;
    }
    
    /**
     * Simple test endpoint for quick testing.
     *
     * @param message User message
     * @return SSE emitter for streaming response
     */
    @GetMapping(value = "/test/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Test streaming chat", description = "Quick test endpoint for streaming chat")
    public SseEmitter testStream(@RequestParam(defaultValue = "你好") String message) {
        log.info("Test streaming chat with message: {}", message);
        
        List<ChatMessageDTO> messages = List.of(
                new ChatMessageDTO("user", message)
        );
        
        SseEmitter emitter = new SseEmitter(300000L);
        
        emitter.onCompletion(() -> log.info("Test SSE completed"));
        emitter.onTimeout(() -> {
            log.warn("Test SSE timeout");
            emitter.complete();
        });
        emitter.onError((ex) -> {
            log.error("Test SSE error", ex);
            emitter.completeWithError(ex);
        });
        
        glmService.chatStream(messages, emitter);
        
        return emitter;
    }
}
