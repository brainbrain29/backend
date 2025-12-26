package com.pandora.backend.agent.model;

import com.pandora.backend.dto.ChatMessageDTO;

import java.util.List;

public record AgentChatSession(
                String sessionId,
                Integer userId,
                List<ChatMessageDTO> history,
                Long updatedAtEpochMs) {
}
