package com.pandora.backend.service;

import com.pandora.backend.repository.EmployeeRepository;
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
import com.pandora.backend.enums.Status;
import com.pandora.backend.enums.Position;

import java.time.LocalDateTime;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.enums.NoticeType;

//TODO:检查Redis缓存逻辑
@Service
public class NoticeService {
    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

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
        notice.setNoticeType((byte) com.pandora.backend.enums.NoticeType.NEW_TASK.getCode());
        String taskTitle = task.getTitle() != null ? task.getTitle() : "";
        notice.setContent("你被指派了任务: " + taskTitle);
        notice.setRelatedId(task.getTaskId()); // 保存关联的任务ID
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
     * 2. 创建任务状态更新通知
     * 
     * @param task      已更新状态的任务实体
     * @param updater   执行更新操作的员工实体
     * @param receiver  通知接收人（可能是负责人，也可能是任务发布人）
     * @param oldStatus 旧的任务状态
     * @param newStatus 新的任务状态
     */
    public void createTaskUpdateNotice(Task task, Employee updater, Employee receiver, Status oldStatus,
            Status newStatus) {
        if (receiver == null || receiver.getEmployeeId() == null) {
            return; // 没有明确接收人时不发送通知
        }
        // 如果更新者和接收人是同一个人，不发送通知
        if (updater.getEmployeeId().equals(receiver.getEmployeeId())) {
            return;
        }

        // 1. 构建通知内容
        String title = task.getTitle() != null ? task.getTitle() : "";
        String oldStatusDesc = oldStatus != null ? oldStatus.getDesc() : "";
        String newStatusDesc = newStatus != null ? newStatus.getDesc() : "";

        String baseContent = String.format("你的任务 '%s' 从 '%s' 更新为 '%s'", title, oldStatusDesc, newStatusDesc);

        String roleName = Position.getDescriptionByCode(updater.getPosition());
        String updaterName = updater.getEmployeeName();
        String extraContent = "";

        if (oldStatus == Status.PENDING_REVIEW && newStatus == Status.COMPLETED) {
            extraContent = String.format("%s %s 通过了你的任务提交", roleName, updaterName);
        } else if (oldStatus == Status.PENDING_REVIEW && newStatus == Status.NOT_FINISHED) {
            extraContent = String.format("%s %s 拒绝了你的任务提交", roleName, updaterName);
        }

        String finalContent = extraContent.isEmpty() ? baseContent : baseContent + "，" + extraContent;

        // 2. 保存通知到数据库
        Notice notice = new Notice();
        notice.setSender(updater); // 操作者是发送方
        notice.setNoticeType((byte) NoticeType.TASK_UPDATE.getCode()); // 使用枚举
        notice.setContent(finalContent);
        notice.setRelatedId(task.getTaskId()); // 保存关联的任务ID
        notice.setCreatedTime(LocalDateTime.now());
        Notice savedNotice = noticeRepository.save(notice);

        // 3. 创建通知与接收者的关联关系
        NoticeEmployee ne = createNoticeEmployeeLink(savedNotice, receiver);

        // 4. 转换为 DTO 并推送/缓存
        sendAndCacheNotice(ne);

        System.out.println("任务状态更新通知已创建并推送给用户: " + receiver.getEmployeeId());
    }

    /**
     * 3. 创建并广播公司重要事项通知
     * 
     * @param createdNotice 已创建的重要事项实体
     * @param matterId      重要事项ID
     */
    public void createCompanyMatterNotice(Notice createdNotice, Integer matterId) {
        // 设置 relatedId
        createdNotice.setRelatedId(matterId);
        noticeRepository.save(createdNotice);

        // 获取所有员工 (在实际生产中，这里可能需要优化为分页处理，避免一次性加载过多用户)
        List<Employee> allEmployees = employeeRepository.findAll();

        // 为每个员工创建通知
        for (Employee employee : allEmployees) {
            // 自己不给自己发通知
            if (employee.getEmployeeId().equals(createdNotice.getSender().getEmployeeId())) {
                continue;
            }

            // 1. 创建通知与接收者的关联关系
            NoticeEmployee ne = createNoticeEmployeeLink(createdNotice, employee);

            // 2. 转换为 DTO 并推送/缓存
            sendAndCacheNotice(ne);
        }

        System.out.println("公司重要事项 '" + createdNotice.getNoticeId() + "' 已广播给 " + (allEmployees.size() - 1) + " 个用户");
    }

    /**
     * 4. 创建并广播公司重要任务通知
     * 
     * @param importantTask 已创建的重要任务实体
     */
    public void createImportantTaskNotice(Task importantTask) {
        // 同样，广播给所有员工
        List<Employee> allEmployees = employeeRepository.findAll();

        for (Employee employee : allEmployees) {
            // 发送者不接收通知
            if (employee.getEmployeeId().equals(importantTask.getSender().getEmployeeId())) {
                continue;
            }

            // 1. 先为这个任务创建一个独立的通知实体
            Notice notice = new Notice();
            notice.setSender(importantTask.getSender());
            notice.setNoticeType((byte) NoticeType.IMPORTANT_TASK.getCode());
            String content = String.format("公司发布了新的重要任务: '%s'", importantTask.getTitle());
            notice.setContent(content);
            notice.setRelatedId(importantTask.getTaskId()); // 保存关联的任务ID
            notice.setCreatedTime(LocalDateTime.now());
            Notice savedNotice = noticeRepository.save(notice);

            // 2. 创建通知与接收者的关联关系
            NoticeEmployee ne = createNoticeEmployeeLink(savedNotice, employee);

            // 3. 转换为 DTO 并推送/缓存
            sendAndCacheNotice(ne);
        }
        System.out.println("公司重要任务 '" + importantTask.getTitle() + "' 已广播给 " + (allEmployees.size() - 1) + " 个用户");
    }

    // ==== 新增的、用于重构和简化代码的私有辅助方法 ====

    /**
     * 辅助方法：创建 Notice 和 Employee 的关联记录
     */
    private NoticeEmployee createNoticeEmployeeLink(Notice notice, Employee receiver) {
        NoticeEmployee ne = new NoticeEmployee();
        NoticeEmployeeId id = new NoticeEmployeeId(notice.getNoticeId(), receiver.getEmployeeId());
        ne.setId(id);
        ne.setNotice(notice);
        ne.setReceiver(receiver);
        ne.setNoticeStatus(NoticeStatus.NOT_VIEWED);
        return noticeEmployeeRepository.save(ne);
    }

    /**
     * 辅助方法：发送并缓存通知
     */
    private void sendAndCacheNotice(NoticeEmployee ne) {
        NoticeDTO dto = toDTO(ne);
        Integer receiverId = ne.getReceiver().getEmployeeId();

        // 更新 Redis 缓存
        cacheService.incrementUnreadCount(receiverId);
        cacheService.cacheRecentNotice(receiverId, dto);

        // SSE 实时推送
        pushService.pushNotification(receiverId, dto);
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
        dto.setTitle(
                n.getNoticeType() != null ? com.pandora.backend.enums.NoticeType.fromCode(n.getNoticeType()).getDesc()
                        : null);
        dto.setContent(n.getContent());
        dto.setSenderName(n.getSender() != null ? n.getSender().getEmployeeName() : null);
        dto.setCreatedTime(n.getCreatedTime());
        dto.setStatus(ne.getNoticeStatus() != null ? ne.getNoticeStatus().getDesc() : null);
        dto.setRelatedId(n.getRelatedId()); // 设置关联ID
        return dto;
    }
}
