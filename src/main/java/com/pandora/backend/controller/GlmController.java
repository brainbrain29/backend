package com.pandora.backend.controller;

import com.pandora.backend.dto.ChatMessageDTO;
import com.pandora.backend.dto.ChatRequestDTO;
import com.pandora.backend.dto.ChatResponseDTO;
import com.pandora.backend.agent.service.WorkReportAgentService;
import com.pandora.backend.service.GlmService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * GLM-4.6 API 集成控制器
 */
@Slf4j
@RestController
@RequestMapping("/glm")
@RequiredArgsConstructor
@Tag(name = "GLM API", description = "GLM-4.6 聊天 API 接口")
public class GlmController {

    private final GlmService glmService;
    private final WorkReportAgentService workReportAgentService;

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

        SseEmitter emitter = new SseEmitter(30000L); // 半分钟超时

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
     * 带上下文的流式聊天接口（包含固定提示词 + 近三周日志任务上下文）
     *
     * @param userId  用户ID
     * @param request 包含消息的聊天请求
     * @return 用于流式响应的 SSE 发射器
     */
    @PostMapping(value = "/chat/stream/context", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "带上下文的流式聊天", description = "发送聊天请求并通过 SSE 获取流式响应，自动加载用户近三周的日志和任务作为上下文")
    public SseEmitter chatStreamWithContext(
            @RequestParam Integer userId,
            @RequestBody ChatRequestDTO request) {
        log.info("收到用户 {} 的带上下文流式聊天请求，包含 {} 条消息",
                userId, request.getMessages().size());

        SseEmitter emitter = new SseEmitter(60000L); // 1分钟超时

        emitter.onCompletion(() -> log.info("用户 {} 的 SSE 完成", userId));
        emitter.onTimeout(() -> {
            log.warn("用户 {} 的 SSE 超时", userId);
            emitter.complete();
        });
        emitter.onError((ex) -> {
            log.error("用户 {} 的 SSE 错误", userId, ex);
            emitter.completeWithError(ex);
        });

        glmService.chatStreamWithContext(userId, request.getMessages(), emitter);

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
                new ChatMessageDTO("user", message));

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

    /**
     * 带上下文的测试接口
     *
     * @param userId  用户ID
     * @param message 用户消息
     * @return 用于流式响应的 SSE 发射器
     */
    @GetMapping(value = "/test/stream/context", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "测试带上下文的流式聊天", description = "用于测试带上下文的流式聊天，自动加载用户近三周的日志和任务")
    public SseEmitter testStreamWithContext(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "帮我分析一下最近的工作情况") String message) {
        log.info("测试用户 {} 的带上下文流式聊天，消息: {}", userId, message);

        List<ChatMessageDTO> messages = List.of(
                new ChatMessageDTO("user", message));

        SseEmitter emitter = new SseEmitter(60000L);

        emitter.onCompletion(() -> log.info("测试 SSE 完成"));
        emitter.onTimeout(() -> {
            log.warn("测试 SSE 超时");
            emitter.complete();
        });
        emitter.onError((ex) -> {
            log.error("测试 SSE 错误", ex);
            emitter.completeWithError(ex);
        });

        glmService.chatStreamWithContext(userId, messages, emitter);

        return emitter;
    }

    /**
     * AI 工作分析接口（从 token 获取员工 ID）
     * 实时生成员工的任务完成趋势、工作节奏建议和情绪健康提醒
     *
     * @param request HTTP 请求（从中获取 token 解析的员工 ID）
     * @return 用于流式响应的 SSE 发射器
     */
    @GetMapping(value = "/analysis/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 工作分析", description = "基于员工近三周的日志和任务数据，实时生成任务完成趋势、工作节奏建议和情绪健康提醒")
    public ResponseEntity<SseEmitter> getAiAnalysis(HttpServletRequest request) {
        // 从 token 中获取员工 ID
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj == null) {
            return ResponseEntity.status(401).build();
        }
        Integer userId = (Integer) userIdObj;

        log.info("收到员工 {} 的 AI 工作分析请求", userId);

        SseEmitter emitter = new SseEmitter(90000L); // 1.5分钟超时

        emitter.onCompletion(() -> log.info("员工 {} 的 AI 分析 SSE 完成", userId));
        emitter.onTimeout(() -> {
            log.warn("员工 {} 的 AI 分析 SSE 超时", userId);
            emitter.complete();
        });
        emitter.onError((ex) -> {
            log.error("员工 {} 的 AI 分析 SSE 错误", userId, ex);
            emitter.completeWithError(ex);
        });

        // 调用 Service 生成 AI 分析
        workReportAgentService.generateWorkReport(userId, emitter);

        return ResponseEntity.ok(emitter);
    }
}
