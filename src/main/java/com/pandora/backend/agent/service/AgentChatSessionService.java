package com.pandora.backend.agent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandora.backend.agent.model.AgentChatSession;
import com.pandora.backend.dto.ChatMessageDTO;
import com.pandora.backend.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AgentChatSessionService {

    private static final String SESSION_KEY_PREFIX = "agent_chat:session:";
    private static final long SESSION_TTL_DAYS = 7;
    private static final int MAX_HISTORY_MESSAGES = 20;

    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper;

    public AgentChatSession loadOrCreateSession(final Integer userId, final String sessionId) {
        final String key = buildSessionKey(userId, sessionId);
        final Object raw = redisUtil.get(key);
        if (raw == null) {
            final AgentChatSession created = new AgentChatSession(sessionId, userId, List.of(),
                    System.currentTimeMillis());
            redisUtil.set(key, created, SESSION_TTL_DAYS, TimeUnit.DAYS);
            return created;
        }

        if (raw instanceof AgentChatSession) {
            return (AgentChatSession) raw;
        }

        if (raw instanceof Map) {
            return objectMapper.convertValue(raw, AgentChatSession.class);
        }

        return new AgentChatSession(sessionId, userId, List.of(), System.currentTimeMillis());
    }

    public void appendTurn(
            final Integer userId,
            final String sessionId,
            final ChatMessageDTO userMessage,
            final ChatMessageDTO assistantMessage) {

        final AgentChatSession session = loadOrCreateSession(userId, sessionId);
        final List<ChatMessageDTO> nextHistory = new ArrayList<>();
        if (session.history() != null) {
            nextHistory.addAll(session.history());
        }

        nextHistory.add(userMessage);
        nextHistory.add(assistantMessage);

        final List<ChatMessageDTO> trimmed = trimToLast(nextHistory, MAX_HISTORY_MESSAGES);

        final AgentChatSession updated = new AgentChatSession(sessionId, userId, trimmed, System.currentTimeMillis());
        redisUtil.set(buildSessionKey(userId, sessionId), updated, SESSION_TTL_DAYS, TimeUnit.DAYS);
    }

    public List<ChatMessageDTO> getHistory(final Integer userId, final String sessionId) {
        final AgentChatSession session = loadOrCreateSession(userId, sessionId);
        return session.history() == null ? List.of() : session.history();
    }

    private String buildSessionKey(final Integer userId, final String sessionId) {
        return SESSION_KEY_PREFIX + userId + ":" + sessionId;
    }

    private List<ChatMessageDTO> trimToLast(final List<ChatMessageDTO> messages, final int limit) {
        if (messages.size() <= limit) {
            return messages;
        }
        return new ArrayList<>(messages.subList(messages.size() - limit, messages.size()));
    }
}
