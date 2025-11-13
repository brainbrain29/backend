package com.pandora.backend.service;

import com.pandora.backend.entity.NoticeEmployee;
import com.pandora.backend.entity.NoticeEmployeeId;
import com.pandora.backend.enums.NoticeStatus;
import com.pandora.backend.repository.NoticeEmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
     * 用户上线时，将待推送队列中的通知状态更新为已接收
     * NOT_RECEIVED → NOT_VIEWED
     */
    public void updatePendingNoticesStatus(Integer userId) {
        // 1. 获取待推送通知列表
        List<com.pandora.backend.dto.NoticeDTO> pendingNotices = cacheService.getPendingNotices(userId);

        if (pendingNotices.isEmpty()) {
            return;
        }

        System.out.println("🔄 开始更新通知状态，用户: " + userId + ", 数量: " + pendingNotices.size());

        // 2. 批量更新状态
        int updatedCount = 0;
        for (com.pandora.backend.dto.NoticeDTO notice : pendingNotices) {
            NoticeEmployeeId id = new NoticeEmployeeId();
            id.setNoticeId(notice.getNoticeId());
            id.setReceiverId(userId);

            NoticeEmployee ne = noticeEmployeeRepository.findById(id).orElse(null);
            if (ne != null && ne.getNoticeStatus() == NoticeStatus.NOT_RECEIVED) {
                ne.setNoticeStatus(NoticeStatus.NOT_VIEWED);
                noticeEmployeeRepository.save(ne);
                updatedCount++;
            }
        }

        System.out.println("✅ 已更新 " + updatedCount + " 条通知状态为已接收，用户: " + userId);
    }
}
