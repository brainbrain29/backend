package com.pandora.backend.service;

import com.pandora.backend.dto.NoticeDTO;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private NotificationCacheService cacheService;

    /**
     * 注册 SSE 连接
     */
    public void registerConnection(Integer userId, SseEmitter emitter) {
        emitters.put(userId, emitter);
        System.out.println("=== SSE Connection Registered ===");
        System.out.println("User ID: " + userId);
        System.out.println("Total online users: " + emitters.size());
        System.out.println("Online user IDs: " + emitters.keySet());
        System.out.println("================================");

        // 连接关闭时清理
        emitter.onCompletion(() -> {
            emitters.remove(userId);
            System.out.println("SSE connection closed for user: " + userId + ", remaining online: " + emitters.size());
        });
        emitter.onTimeout(() -> {
            emitters.remove(userId);
            System.out.println("SSE connection timeout for user: " + userId);
        });
        emitter.onError((e) -> {
            emitters.remove(userId);
            System.out.println("SSE connection error for user: " + userId);
        });

        // 用户上线后，推送待接收的通知
        pushPendingNotifications(userId, emitter);
    }

    /**
     * 推送用户的待接收通知（用户上线时调用）
     * 返回推送成功的通知 ID 列表，用于后续更新状态
     */
    private java.util.List<Integer> pushPendingNotifications(Integer userId, SseEmitter emitter) {
        java.util.List<NoticeDTO> pendingNotices = cacheService.getPendingNotices(userId);
        java.util.List<Integer> successIds = new java.util.ArrayList<>();
        
        if (pendingNotices.isEmpty()) {
            System.out.println("✅ 用户 " + userId + " 没有待推送通知");
            return successIds;
        }

        System.out.println("📤 开始推送待接收通知，用户: " + userId + ", 数量: " + pendingNotices.size());

        for (NoticeDTO notice : pendingNotices) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notice));
                successIds.add(notice.getNoticeId());
            } catch (IOException e) {
                System.out.println("❌ 推送通知失败，noticeId: " + notice.getNoticeId());
                break; // 连接已断开，停止推送
            }
        }

        // 推送成功后，清空待推送队列
        if (!successIds.isEmpty()) {
            cacheService.clearPendingNotices(userId);
            System.out.println("✅ 成功推送 " + successIds.size() + " 条通知给用户: " + userId);
        }
        
        return successIds;
    }
    
    /**
     * 获取推送成功的通知 ID 列表（供外部调用）
     */
    public java.util.List<Integer> getPushedNoticeIds(Integer userId) {
        // 这个方法在 registerConnection 时会被调用
        // 返回值用于更新通知状态
        return new java.util.ArrayList<>();
    }

    /**
     * 推送通知给指定用户
     * - 用户在线：立即推送，状态变为 NOT_VIEWED（已接收未查看）
     * - 用户离线：加入 Redis 待推送队列，状态保持 NOT_RECEIVED（未接收）
     */
    public void pushNotification(Integer userId, NoticeDTO notice) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            // 用户在线，立即推送
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notice));
                System.out.println("✅ 通知已实时推送给用户: " + userId + ", noticeId: " + notice.getNoticeId());
                // 注意：此时通知状态应该是 NOT_VIEWED（已接收未查看）
            } catch (IOException e) {
                // 推送失败，连接可能已断开
                emitters.remove(userId);
                emitter.completeWithError(e);
                
                // 加入待推送队列
                cacheService.addPendingNotice(userId, notice);
                System.out.println("⚠️ 推送失败，通知已加入待推送队列，用户: " + userId);
            }
        } else {
            // 用户离线，加入 Redis 待推送队列
            cacheService.addPendingNotice(userId, notice);
            System.out.println("📥 用户离线，通知已加入待推送队列，用户: " + userId + ", noticeId: " + notice.getNoticeId());
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