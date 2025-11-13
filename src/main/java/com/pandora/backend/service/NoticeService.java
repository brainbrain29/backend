package com.pandora.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import com.pandora.backend.repository.NoticeRepository;
import com.pandora.backend.repository.NoticeEmployeeRepository;
import com.pandora.backend.dto.NoticeDTO;
import com.pandora.backend.dto.NoticeStatusDTO;
import com.pandora.backend.entity.Notice;
import com.pandora.backend.entity.NoticeEmployee;
import com.pandora.backend.entity.NoticeEmployeeId;
import com.pandora.backend.entity.Task;
import com.pandora.backend.enums.NoticeStatus;

import java.time.LocalDateTime;

//TODO:检查Redis缓存逻辑
@Service
public class NoticeService {
    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private NoticeEmployeeRepository noticeEmployeeRepository;

    @Autowired
    private NotificationCacheService cacheService;

    @Autowired
    private NotificationPushService pushService;

    @Autowired
    @Qualifier("asyncExecutor")
    private Executor asyncExecutor; // 异步线程池

    /**
     * 获取所有通知（包括已读和未读）
     * 注意：此接口不使用 Redis 缓存，因为：
     * 1. 数据量可能很大（历史通知）
     * 2. 查询频率低（用户不常查看历史通知）
     * 3. 缓存收益低（缓存大量数据占用内存）
     */
    public List<NoticeDTO> getAllNotice(Integer userId) {
        List<NoticeEmployee> list = noticeEmployeeRepository.findAllByReceiverId(userId);
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * 获取未读通知列表（优先从 Redis 读取最近通知）
     * 优化：异步写入 Redis，不阻塞用户响应
     */
    public List<NoticeDTO> getUnreadNotice(Integer userId) {
        // 1. 先从 Redis 读取最近通知（5分钟缓存）
        List<NoticeDTO> cachedNotices = cacheService.getRecentNotices(userId);

        // 2. 如果 Redis 有数据，直接返回
        if (cachedNotices != null && !cachedNotices.isEmpty()) {
            System.out.println("✅ 从 Redis 读取通知，用户: " + userId);
            return cachedNotices;
        }

        // 3. Redis 没有数据，查询 MySQL
        System.out.println("⚠️ Redis 未命中，从 MySQL 读取通知，用户: " + userId);
        List<NoticeEmployee> list = noticeEmployeeRepository.findUnreadByReceiverId(userId);
        List<NoticeDTO> notices = list.stream().map(this::toDTO).collect(Collectors.toList());

        // 4. 异步写入 Redis 缓存（不阻塞用户响应）🚀
        if (!notices.isEmpty()) {
            final List<NoticeDTO> finalNotices = notices; // Lambda 需要 final 变量
            asyncExecutor.execute(() -> {
                System.out.println("🔄 异步写入 Redis 缓存，用户: " + userId);
                cacheService.cacheRecentNotices(userId, finalNotices); // 批量写入
                System.out.println("✅ Redis 缓存写入完成，用户: " + userId);
            });
        }

        // 5. 立即返回数据（不等待 Redis 写入）
        return notices;
    }

    /**
     * 检查未读通知数量（优先从 Redis 读取）
     */
    public NoticeStatusDTO checkUnreadNotice(Integer userId) {
        // 1. 先从 Redis 读取
        Long cachedCount = cacheService.getUnreadCount(userId);

        // 2. 如果 Redis 有数据，直接返回
        if (cachedCount != null && cachedCount > 0) {
            return new NoticeStatusDTO(true, cachedCount.intValue());
        }

        // 3. Redis 没有数据，查询 MySQL
        long count = noticeEmployeeRepository.countUnreadByReceiverId(userId);

        // 4. 写入 Redis 缓存（直接 set 值，不用循环 increment）
        if (count > 0) {
            cacheService.setUnreadCount(userId, count);
        }

        return new NoticeStatusDTO(count > 0, (int) count);
    }

    /**
     * 创建任务分配通知（集成 Redis 缓存 + SSE 推送）
     */
    public void createTaskAssignmentNotice(Task task) {
        if (task.getAssignee() == null || task.getSender() == null) {
            return;
        }

        // 如果分配者和执行者是同一人,不发送通知
        if (task.getAssignee().getEmployeeId().equals(task.getSender().getEmployeeId())) {
            return;
        }

        // 1. 保存通知到数据库
        Notice notice = new Notice();
        notice.setSender(task.getSender());
        notice.setNoticeType((byte) 1);
        String title = task.getTitle() != null ? task.getTitle() : "";
        notice.setContent("你被指派了任务: " + title);
        notice.setCreatedTime(LocalDateTime.now());
        Notice saved = noticeRepository.save(notice);

        Integer receiverId = task.getAssignee().getEmployeeId();
        
        // 判断用户是否在线，设置初始状态
        boolean isOnline = pushService.isUserOnline(receiverId);
        NoticeStatus initialStatus = isOnline ? NoticeStatus.NOT_VIEWED : NoticeStatus.NOT_RECEIVED;
        
        NoticeEmployee ne = new NoticeEmployee();
        NoticeEmployeeId id = new NoticeEmployeeId();
        id.setNoticeId(saved.getNoticeId());
        id.setReceiverId(receiverId);
        ne.setId(id);
        ne.setNotice(saved);
        ne.setReceiver(task.getAssignee());
        ne.setNoticeStatus(initialStatus);
        noticeEmployeeRepository.save(ne);

        // 2. 转换为 DTO
        NoticeDTO dto = toDTO(ne);

        // 3. 更新 Redis 缓存
        cacheService.incrementUnreadCount(receiverId); // 未读数 +1
        cacheService.cacheRecentNotice(receiverId, dto); // 缓存最近通知

        // 4. SSE 实时推送（如果用户在线）或加入待推送队列（用户离线）
        pushService.pushNotification(receiverId, dto);

        System.out.println("任务分配通知已创建，用户: " + receiverId + ", 状态: " + initialStatus.getDesc());
    }

    /**
     * 批量更新通知状态为已接收（用户上线推送后调用）
     * NOT_RECEIVED → NOT_VIEWED
     */
    public void markAsReceived(Integer userId, java.util.List<Integer> noticeIds) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            return;
        }

        for (Integer noticeId : noticeIds) {
            NoticeEmployeeId id = new NoticeEmployeeId();
            id.setNoticeId(noticeId);
            id.setReceiverId(userId);

            NoticeEmployee ne = noticeEmployeeRepository.findById(id).orElse(null);
            if (ne != null && ne.getNoticeStatus() == NoticeStatus.NOT_RECEIVED) {
                ne.setNoticeStatus(NoticeStatus.NOT_VIEWED);
                noticeEmployeeRepository.save(ne);
            }
        }

        System.out.println("✅ 已更新 " + noticeIds.size() + " 条通知状态为已接收，用户: " + userId);
    }

    /**
     * 标记单个通知为已读
     * 使用分布式锁防止读到脏数据
     */
    public void markAsRead(Integer userId, Integer noticeId) {
        String lockKey = "lock:notice:" + userId;
        String lockValue = java.util.UUID.randomUUID().toString();
        
        try {
            // 1. 获取分布式锁（超时5秒）
            Boolean locked = cacheService.tryLock(lockKey, lockValue, 5, java.util.concurrent.TimeUnit.SECONDS);
            if (!locked) {
                System.out.println("⚠️ 获取锁失败，用户: " + userId + " 可能有并发操作");
                throw new RuntimeException("操作过于频繁，请稍后重试");
            }
            
            // 2. 先删除缓存（防止读到旧数据）
            cacheService.clearAllCache(userId);
            
            // 3. 更新数据库
            NoticeEmployeeId id = new NoticeEmployeeId();
            id.setNoticeId(noticeId);
            id.setReceiverId(userId);
            
            NoticeEmployee ne = noticeEmployeeRepository.findById(id).orElse(null);
            if (ne != null && ne.getNoticeStatus() == NoticeStatus.NOT_VIEWED) {
                ne.setNoticeStatus(NoticeStatus.VIEWED);
                noticeEmployeeRepository.save(ne);
                
                System.out.println("✅ 通知已标记为已读，noticeId: " + noticeId + ", 用户: " + userId);
            }
            
            // 4. 再次删除缓存（延迟双删）
            cacheService.clearAllCache(userId);
            
        } finally {
            // 5. 释放锁
            cacheService.releaseLock(lockKey, lockValue);
        }
    }

    /**
     * 标记所有通知为已读
     * 使用分布式锁防止读到脏数据
     */
    public void markAllAsRead(Integer userId) {
        String lockKey = "lock:notice:" + userId;
        String lockValue = java.util.UUID.randomUUID().toString();
        
        try {
            // 1. 获取分布式锁
            Boolean locked = cacheService.tryLock(lockKey, lockValue, 5, java.util.concurrent.TimeUnit.SECONDS);
            if (!locked) {
                System.out.println("⚠️ 获取锁失败，用户: " + userId + " 可能有并发操作");
                throw new RuntimeException("操作过于频繁，请稍后重试");
            }
            
            // 2. 先删除缓存
            cacheService.clearAllCache(userId);
            
            // 3. 更新数据库
            List<NoticeEmployee> unreadNotices = noticeEmployeeRepository.findUnreadByReceiverId(userId);
            if (!unreadNotices.isEmpty()) {
                for (NoticeEmployee ne : unreadNotices) {
                    ne.setNoticeStatus(NoticeStatus.VIEWED);
                }
                noticeEmployeeRepository.saveAll(unreadNotices);
                
                System.out.println("✅ 所有通知已标记为已读，数量: " + unreadNotices.size() + ", 用户: " + userId);
            }
            
            // 4. 再次删除缓存
            cacheService.clearAllCache(userId);
            
        } finally {
            // 5. 释放锁
            cacheService.releaseLock(lockKey, lockValue);
        }
    }

    /**
     * 删除通知
     */
    public void deleteNotice(Integer userId, Integer noticeId) {
        NoticeEmployeeId id = new NoticeEmployeeId();
        id.setNoticeId(noticeId);
        id.setReceiverId(userId);

        NoticeEmployee ne = noticeEmployeeRepository.findById(id).orElse(null);
        if (ne != null) {
            // 1. 如果通知未读，更新未读数量
            if (ne.getNoticeStatus() == NoticeStatus.NOT_VIEWED) {
                cacheService.decrementUnreadCount(userId);
            }

            // 2. 从数据库删除
            noticeEmployeeRepository.delete(ne);

            // 3. 清空缓存
            cacheService.clearAllCache(userId);

            System.out.println("Notification " + noticeId + " deleted for user: " + userId);
        }
    }

    private NoticeDTO toDTO(NoticeEmployee ne) {
        Notice n = ne.getNotice();
        NoticeDTO dto = new NoticeDTO();
        dto.setNoticeId(n.getNoticeId());
        dto.setContent(n.getContent());
        dto.setSenderName(n.getSender() != null ? n.getSender().getEmployeeName() : null);
        dto.setCreatedTime(n.getCreatedTime());
        dto.setStatus(ne.getNoticeStatus() != null ? ne.getNoticeStatus().getCode() : null);
        return dto;
    }
}
