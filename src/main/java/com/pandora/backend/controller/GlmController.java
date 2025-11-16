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
 * GLM-4.6 API 集成控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/glm")
@RequiredArgsConstructor
@Tag(name = "GLM API", description = "GLM-4.6 聊天 API 接口")
public class GlmController {
    
    private final GlmService glmService;
    
    /**
     * 非流式聊天接口
     *
     * @param request 包含消息的聊天请求
     * @return 完整的聊天响应
     */
    @PostMapping("/chat")
    @Operation(summary = "非流式聊天", description = "发送聊天请求并获取完整响应")
    public ResponseEntity<ChatResponseDTO> chat(@RequestBody ChatRequestDTO request) {
        log.info("收到非流式聊天请求,包含 {} 条消息", 
                request.getMessages().size());
        
        ChatResponseDTO response = glmService.chat(request.getMessages());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 使用 SSE 的流式聊天接口
     *
     * @param request 包含消息的聊天请求
     * @return 用于流式响应的 SSE 发射器
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式聊天", description = "发送聊天请求并通过 SSE 获取流式响应")
    public SseEmitter chatStream(@RequestBody ChatRequestDTO request) {
        log.info("收到流式聊天请求,包含 {} 条消息", 
                request.getMessages().size());
        
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        
        emitter.onCompletion(() -> log.info("SSE 完成"));
        emitter.onTimeout(() -> {
            log.warn("SSE 超时");
            emitter.complete();
        });
        emitter.onError((ex) -> {
            log.error("SSE 错误", ex);
            emitter.completeWithError(ex);
        });
        
        glmService.chatStream(request.getMessages(), emitter);
        
        return emitter;
    }
    
    /**
     * 用于快速测试的简单测试接口
     *
     * @param message 用户消息
     * @return 用于流式响应的 SSE 发射器
     */
    @GetMapping(value = "/test/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "测试流式聊天", description = "用于流式聊天的快速测试接口")
    public SseEmitter testStream(@RequestParam(defaultValue = "你好") String message) {
        log.info("测试流式聊天,消息: {}", message);
        
        List<ChatMessageDTO> messages = List.of(
                new ChatMessageDTO("user", message)
        );
        
        SseEmitter emitter = new SseEmitter(300000L);
        
        emitter.onCompletion(() -> log.info("测试 SSE 完成"));
        emitter.onTimeout(() -> {
            log.warn("测试 SSE 超时");
            emitter.complete();
        });
        emitter.onError((ex) -> {
            log.error("测试 SSE 错误", ex);
            emitter.completeWithError(ex);
        });
        
        glmService.chatStream(messages, emitter);
        
        return emitter;
    }
}
