package com.pandora.backend.service;

import com.pandora.backend.dto.NoticeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(NotificationPushService.class);

    // 存储所有活跃的 SSE 连接：userId -> SseEmitter
    private static final Map<Integer, SseEmitter> emitters = new ConcurrentHashMap<>();

    @Autowired
    private NotificationCacheService cacheService;

    /**
     * 注册 SSE 连接
     */
    public void registerConnection(Integer userId, String userName, SseEmitter emitter) {
        emitters.put(userId, emitter);

        // ✅ 使用 Redis 管理在线状态
        cacheService.setUserOnline(userId);

        logger.info("SSE 连接已建立 - 用户: {} (ID: {}), 当前在线: {}", userName, userId, emitters.size());

        // 连接关闭时清理
        emitter.onCompletion(() -> {
            try {
                emitters.remove(userId);
                // ✅ 设置用户离线
                cacheService.setUserOffline(userId);
                logger.info("用户下线，userId: {}, 剩余在线: {}", userId, emitters.size());
            } catch (Exception e) {
                logger.error("处理 SSE 连接关闭时发生异常，userId: {}", userId, e);
            }
        });
        emitter.onTimeout(() -> {
            try {
                emitters.remove(userId);
                // ✅ 设置用户离线
                cacheService.setUserOffline(userId);
                logger.info("SSE 连接超时，userId: {}", userId);
            } catch (Exception e) {
                logger.error("处理 SSE 连接超时时发生异常，userId: {}", userId, e);
            }
        });
        emitter.onError((e) -> {
            try {
                emitters.remove(userId);
                // ✅ 设置用户离线
                cacheService.setUserOffline(userId);
                logger.debug("SSE 连接异常，userId: {} - {}", userId, e.getMessage());
            } catch (Exception ex) {
                logger.error("处理 SSE 连接错误时发生异常，userId: {}", userId, ex);
            }
        });

        // 用户上线后，推送待接收的通知
        pushPendingNotifications(userId, emitter);
    }

    /**
     * 推送用户的待接收通知（用户上线时调用）
     * 返回推送成功的通知 ID 列表，用于后续更新状态
     * 
     * 注意：不立即清空 Redis 队列，等待状态更新完成后再清空
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

        // ⚠️ 不在这里清空队列，改为在状态更新完成后清空
        // 这样 updatePendingNoticesStatus() 可以从 Redis 获取到通知列表
        if (!successIds.isEmpty()) {
            System.out.println("✅ 成功推送 " + successIds.size() + " 条通知给用户: " + userId);
            System.out.println("📋 待推送队列暂不清空，等待状态更新完成");
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
     * 保持 SSE 连接活跃，并刷新 Redis 在线状态
     */
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        int onlineCount = emitters.size();
        System.out.println("💓 心跳检测 - 当前在线用户数: " + onlineCount);

        // ✅ 刷新所有在线用户的 Redis 状态（延长过期时间）
        for (Integer userId : emitters.keySet()) {
            cacheService.refreshUserOnline(userId);
        }

        if (onlineCount == 0) {
            return;
        }

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
     * 判断用户是否在线
     * ✅ 使用 Redis 判断（支持分布式）
     */
    public boolean isUserOnline(Integer userId) {
        return cacheService.isUserOnline(userId);
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