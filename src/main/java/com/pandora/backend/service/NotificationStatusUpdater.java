package com.pandora.backend.service;

import com.pandora.backend.entity.NoticeEmployee;
import com.pandora.backend.enums.NoticeStatus;
import com.pandora.backend.repository.NoticeEmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通知状态更新服务
 * 负责处理通知状态转换逻辑，避免循环依赖
 */
@Service
public class NotificationStatusUpdater {

    @Autowired
    private NoticeEmployeeRepository noticeEmployeeRepository;

    @Autowired
    private NotificationCacheService cacheService;

    /**
     * 用户上线时，将待推送队列中的通知状态更新为"未查看"
     * NOT_RECEIVED → NOT_VIEWED
     * 
     * 方案A：使用 Redis 队列（推荐）
     * - 从 Redis 获取待推送通知列表
     * - 更新数据库中对应通知的状态
     * - 更新完成后清空 Redis 队列
     * 
     * 方案B：直接查询数据库（备用方案，如果 Redis 失效）
     * - 查询所有 NOT_RECEIVED 状态的通知
     * - 批量更新为 NOT_VIEWED
     * 
     * ⚠️ 重要：添加 @Transactional 注解确保数据库连接正确释放
     */
    @Transactional
    public void updatePendingNoticesStatus(Integer userId) {
        System.out.println("🔄 开始更新通知状态，用户: " + userId);

        // 方案A：优先使用 Redis 队列
        List<com.pandora.backend.dto.NoticeDTO> pendingNotices = cacheService.getPendingNotices(userId);

        if (!pendingNotices.isEmpty()) {
            System.out.println("📋 从 Redis 获取到 " + pendingNotices.size() + " 条待推送通知");

            // 批量更新状态
            int updatedCount = 0;
            for (com.pandora.backend.dto.NoticeDTO notice : pendingNotices) {
                com.pandora.backend.entity.NoticeEmployeeId id = new com.pandora.backend.entity.NoticeEmployeeId();
                id.setNoticeId(notice.getNoticeId());
                id.setReceiverId(userId);

                NoticeEmployee ne = noticeEmployeeRepository.findById(id).orElse(null);
                if (ne != null && ne.getNoticeStatus() == NoticeStatus.NOT_RECEIVED) {
                    ne.setNoticeStatus(NoticeStatus.NOT_VIEWED);
                    noticeEmployeeRepository.save(ne);
                    updatedCount++;
                }
            }

            // 更新完成后清空 Redis 队列
            cacheService.clearPendingNotices(userId);
            System.out.println("✅ 已更新 " + updatedCount + " 条通知状态为未查看，用户: " + userId);
            System.out.println("🗑️ Redis 待推送队列已清空");
            return;
        }

        // 方案B：Redis 队列为空时，从数据库查询（兜底方案）
        System.out.println("⚠️ Redis 队列为空，尝试从数据库查询未接收通知");
        List<NoticeEmployee> notReceivedNotices = noticeEmployeeRepository
                .findByIdReceiverIdAndNoticeStatus(userId, NoticeStatus.NOT_RECEIVED);

        if (notReceivedNotices.isEmpty()) {
            System.out.println("✅ 用户 " + userId + " 没有未接收的通知");
            return;
        }

        System.out.println("📋 从数据库找到 " + notReceivedNotices.size() + " 条未接收通知");

        // 批量更新状态为"未查看"
        for (NoticeEmployee ne : notReceivedNotices) {
            ne.setNoticeStatus(NoticeStatus.NOT_VIEWED);
        }
        noticeEmployeeRepository.saveAll(notReceivedNotices);

        System.out.println("✅ 已更新 " + notReceivedNotices.size() + " 条通知状态为未查看（从数据库），用户: " + userId);
    }
}
