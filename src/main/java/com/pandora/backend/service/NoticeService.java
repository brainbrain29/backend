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
        notice.setNoticeType((byte) 1);
        String title = task.getTitle() != null ? task.getTitle() : "";
        notice.setContent("你被指派了任务: " + title);
        notice.setCreatedTime(LocalDateTime.now());
        Notice saved = noticeRepository.save(notice);

        NoticeEmployee ne = new NoticeEmployee();
        NoticeEmployeeId id = new NoticeEmployeeId();
        id.setNoticeId(saved.getNoticeId());
        id.setReceiverId(task.getAssignee().getEmployeeId());
        ne.setId(id);
        ne.setNotice(saved);
        ne.setReceiver(task.getAssignee());
        ne.setNoticeStatus(NoticeStatus.NOT_VIEWED);
        noticeEmployeeRepository.save(ne);

        // 2. 转换为 DTO
        NoticeDTO dto = toDTO(ne);

        // 3. 更新 Redis 缓存
        Integer receiverId = task.getAssignee().getEmployeeId();
        cacheService.incrementUnreadCount(receiverId); // 未读数 +1
        cacheService.cacheRecentNotice(receiverId, dto); // 缓存最近通知

        // 4. SSE 实时推送（如果用户在线）
        pushService.pushNotification(receiverId, dto);

        System.out.println("任务分配通知已创建并推送给用户: " + receiverId);
    }

    /**
     * 2. 创建任务状态更新通知
     * @param task      已更新状态的任务实体
     * @param updater   执行更新操作的员工实体
     */
    public void createTaskUpdateNotice(Task task, Employee updater) {
        if (task.getAssignee() == null) {
            return; // 任务没有负责人，无需通知
        }
        // 如果更新者和负责人是同一个人，不发送通知
        if (updater.getEmployeeId().equals(task.getAssignee().getEmployeeId())) {
            return;
        }

        // 1. 保存通知到数据库
        Notice notice = new Notice();
        notice.setSender(updater); // 操作者是发送方
        notice.setNoticeType((byte) NoticeType.TASK_UPDATE.getCode()); // 使用枚举
        String content = String.format("你的任务 '%s' 状态已更新", task.getTitle());
        notice.setContent(content);
        notice.setCreatedTime(LocalDateTime.now());
        Notice savedNotice = noticeRepository.save(notice);

        // 2. 创建通知与接收者的关联关系
        NoticeEmployee ne = createNoticeEmployeeLink(savedNotice, task.getAssignee());

        // 3. 转换为 DTO 并推送/缓存
        sendAndCacheNotice(ne);

        System.out.println("任务状态更新通知已创建并推送给用户: " + task.getAssignee().getEmployeeId());
    }

    /**
     * 3. 创建并广播公司重要事项通知
     * @param createdNotice 已创建的重要事项实体
     */
    public void createCompanyMatterNotice(Notice createdNotice) {
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

        System.out.println("公司重要事项 '" + createdNotice.getNoticeId() + "' 已广播给 " + (allEmployees.size() -1) + " 个用户");
    }

    /**
     * 4. 创建并广播公司重要任务通知
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
            notice.setCreatedTime(LocalDateTime.now());
            Notice savedNotice = noticeRepository.save(notice);

            // 2. 创建通知与接收者的关联关系
            NoticeEmployee ne = createNoticeEmployeeLink(savedNotice, employee);

            // 3. 转换为 DTO 并推送/缓存
            sendAndCacheNotice(ne);
        }
        System.out.println("公司重要任务 '" + importantTask.getTitle() + "' 已广播给 " + (allEmployees.size() -1) + " 个用户");
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
     */
    public void markAsRead(Integer userId, Integer noticeId) {
        NoticeEmployeeId id = new NoticeEmployeeId();
        id.setNoticeId(noticeId);
        id.setReceiverId(userId);

        NoticeEmployee ne = noticeEmployeeRepository.findById(id).orElse(null);
        if (ne != null && ne.getNoticeStatus() == NoticeStatus.NOT_VIEWED) {
            // 1. 更新数据库
            ne.setNoticeStatus(NoticeStatus.VIEWED);
            noticeEmployeeRepository.save(ne);

            // 2. 更新 Redis 缓存
            cacheService.decrementUnreadCount(userId);
            cacheService.clearAllCache(userId); // 清空缓存，强制重新加载

            System.out.println("Notification " + noticeId + " marked as read for user: " + userId);
        }
    }

    /**
     * 标记所有通知为已读
     */
    public void markAllAsRead(Integer userId) {
        List<NoticeEmployee> unreadNotices = noticeEmployeeRepository.findUnreadByReceiverId(userId);

        if (!unreadNotices.isEmpty()) {
            // 1. 更新数据库
            for (NoticeEmployee ne : unreadNotices) {
                ne.setNoticeStatus(NoticeStatus.VIEWED);
            }
            noticeEmployeeRepository.saveAll(unreadNotices);

            // 2. 清空 Redis 缓存
            cacheService.clearUnreadCount(userId);
            cacheService.clearAllCache(userId);

            System.out.println("All " + unreadNotices.size() + " notifications marked as read for user: " + userId);
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
