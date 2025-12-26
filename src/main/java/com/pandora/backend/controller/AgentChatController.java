package com.pandora.backend.controller;

import com.pandora.backend.agent.service.AgentChatService;
import com.pandora.backend.dto.AgentChatStreamRequestDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentChatController {

    private final AgentChatService agentChatService;

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> chatStream(
            @RequestAttribute("userId") final Integer userId,
            @RequestBody final AgentChatStreamRequestDTO request) {
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        log.info("[agent-chat] request received userId={} sessionId={}", userId, request.getSessionId());
        final SseEmitter emitter = agentChatService.chatStream(userId, request);
        return ResponseEntity.ok(emitter);
    }
}
