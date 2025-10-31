package com.pandora.backend.service;

import com.pandora.backend.dto.NoticeDTO;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知推送服务
 * 管理 SSE 连接并向在线用户推送通知
 */
@Service
public class NotificationPushService {

    // 存储所有活跃的 SSE 连接：userId -> SseEmitter
    private static final Map<Integer, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 注册 SSE 连接
     */
    public void registerConnection(Integer userId, SseEmitter emitter) {
        emitters.put(userId, emitter);

        // 连接关闭时清理
        emitter.onCompletion(() -> {
            emitters.remove(userId);
            System.out.println("SSE connection closed for user: " + userId);
        });
        emitter.onTimeout(() -> {
            emitters.remove(userId);
            System.out.println("SSE connection timeout for user: " + userId);
        });
        emitter.onError((e) -> {
            emitters.remove(userId);
            System.out.println("SSE connection error for user: " + userId);
        });
    }

    /**
     * 推送通知给指定用户
     */
    public void pushNotification(Integer userId, NoticeDTO notice) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notice));
                System.out.println("Notification pushed to user: " + userId);
            } catch (IOException e) {
                emitters.remove(userId);
                emitter.completeWithError(e);
            }
        } else {
            System.out.println("User " + userId + " is offline, notification saved to DB only");
        }
    }

    /**
     * 发送心跳给所有在线用户（每 30 秒）
     * 保持 SSE 连接活跃
     */
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        if (emitters.isEmpty()) {
            return;
        }

        System.out.println("Sending heartbeat to " + emitters.size() + " online users");
        emitters.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("ping"));
            } catch (IOException e) {
                emitters.remove(userId);
                emitter.completeWithError(e);
                System.out.println("Heartbeat failed for user: " + userId);
            }
        });
    }

    /**
     * 获取在线用户数量
     */
    public int getOnlineCount() {
        return emitters.size();
    }

    /**
     * 获取在线用户 ID 列表
     */
    public Set<Integer> getOnlineUserIds() {
        return emitters.keySet();
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Integer userId) {
        return emitters.containsKey(userId);
    }

    /**
     * 关闭指定用户的连接
     */
    public void closeConnection(Integer userId) {
        SseEmitter emitter = emitters.remove(userId);
        if (emitter != null) {
            emitter.complete();
            System.out.println("Manually closed SSE connection for user: " + userId);
        }
    }
}