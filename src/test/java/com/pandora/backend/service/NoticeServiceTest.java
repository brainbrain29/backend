package com.pandora.backend.service;

import com.pandora.backend.dto.NoticeDTO;
import com.pandora.backend.dto.NoticeStatusDTO;
import com.pandora.backend.entity.*;
import com.pandora.backend.enums.NoticeStatus;
import com.pandora.backend.repository.NoticeEmployeeRepository;
import com.pandora.backend.repository.NoticeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NoticeService 单元测试
 * 测试业务逻辑，不依赖真实的数据库和 Redis
 */
@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {
    @Mock
    private Executor asyncExecutor;

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private NoticeEmployeeRepository noticeEmployeeRepository;

    @Mock
    private NotificationCacheService cacheService;

    @Mock
    private NotificationPushService pushService;

    @InjectMocks
    private NoticeService noticeService;

    private Employee sender;
    private Employee receiver;
    private Notice notice;
    private NoticeEmployee noticeEmployee;

    @BeforeEach
    void setUp() {
        // 准备测试数据
        sender = new Employee();
        sender.setEmployeeId(1);
        sender.setEmployeeName("张三");

        receiver = new Employee();
        receiver.setEmployeeId(2);
        receiver.setEmployeeName("李四");

        notice = new Notice();
        notice.setNoticeId(100);
        notice.setContent("你被指派了任务: 完成项目报告");
        notice.setSender(sender);
        notice.setCreatedTime(LocalDateTime.now());

        NoticeEmployeeId id = new NoticeEmployeeId();
        id.setNoticeId(100);
        id.setReceiverId(2);

        noticeEmployee = new NoticeEmployee();
        noticeEmployee.setId(id);
        noticeEmployee.setNotice(notice);
        noticeEmployee.setReceiver(receiver);
        noticeEmployee.setNoticeStatus(NoticeStatus.NOT_VIEWED);
    }

    /**
     * 测试：获取未读通知（Redis 缓存命中）
     */
    @Test
    void testGetUnreadNotice_CacheHit() {
        // 准备数据
        Integer userId = 2;
        List<NoticeDTO> cachedNotices = Arrays.asList(
                createNoticeDTO(100, "通知1", NoticeStatus.NOT_VIEWED.getCode()),
                createNoticeDTO(101, "通知2", NoticeStatus.NOT_VIEWED.getCode()));

        // Mock Redis 返回缓存数据
        when(cacheService.getRecentNotices(userId)).thenReturn(cachedNotices);

        // 执行
        List<NoticeDTO> result = noticeService.getUnreadNotice(userId);

        // 验证
        assertEquals(2, result.size());
        assertEquals("通知1", result.get(0).getContent());

        // 验证没有查询数据库
        verify(noticeEmployeeRepository, never()).findUnreadByReceiverId(any());

        // 验证从 Redis 读取
        verify(cacheService, times(1)).getRecentNotices(userId);
    }

    /**
     * 测试：获取未读通知（Redis 缓存未命中，查询 MySQL）
     */
    @Test
    void testGetUnreadNotice_CacheMiss() {
        // 准备数据
        Integer userId = 2;
        List<NoticeEmployee> dbNotices = Arrays.asList(noticeEmployee);

        // Mock Redis 返回空
        when(cacheService.getRecentNotices(userId)).thenReturn(null);

        // Mock MySQL 返回数据
        when(noticeEmployeeRepository.findUnreadByReceiverId(userId)).thenReturn(dbNotices);

        // 执行
        List<NoticeDTO> result = noticeService.getUnreadNotice(userId);

        // 验证
        assertEquals(1, result.size());
        assertEquals("你被指派了任务: 完成项目报告", result.get(0).getContent());

        // 验证查询了数据库
        verify(noticeEmployeeRepository, times(1)).findUnreadByReceiverId(userId);

        // 验证异步写入 Redis（注意：异步操作难以验证，这里只验证调用）
        verify(cacheService, times(1)).getRecentNotices(userId);
    }

    /**
     * 测试：检查未读数量（Redis 缓存命中）
     */
    @Test
    void testCheckUnreadNotice_CacheHit() {
        // 准备数据
        Integer userId = 2;
        when(cacheService.getUnreadCount(userId)).thenReturn(5L);

        // 执行
        NoticeStatusDTO result = noticeService.checkUnreadNotice(userId);

        // 验证
        assertTrue(result.isHasUnreadNotice());
        assertEquals(5, result.getUnreadCount());

        // 验证没有查询数据库
        verify(noticeEmployeeRepository, never()).countUnreadByReceiverId(any());
    }

    /**
     * 测试：检查未读数量（Redis 缓存未命中）
     */
    @Test
    void testCheckUnreadNotice_CacheMiss() {
        // 准备数据
        Integer userId = 2;
        when(cacheService.getUnreadCount(userId)).thenReturn(null);
        when(noticeEmployeeRepository.countUnreadByReceiverId(userId)).thenReturn(3L);

        // 执行
        NoticeStatusDTO result = noticeService.checkUnreadNotice(userId);

        // 验证
        assertTrue(result.isHasUnreadNotice());
        assertEquals(3, result.getUnreadCount());

        // 验证查询了数据库
        verify(noticeEmployeeRepository, times(1)).countUnreadByReceiverId(userId);

        // 验证写入了 Redis
        verify(cacheService, times(1)).setUnreadCount(userId, 3L);
    }

    /**
     * 测试：标记单个通知为已读
     */
    @Test
    void testMarkAsRead_Success() {
        // 准备数据
        Integer userId = 2;
        Integer noticeId = 100;

        NoticeEmployeeId id = new NoticeEmployeeId();
        id.setNoticeId(noticeId);
        id.setReceiverId(userId);

        when(noticeEmployeeRepository.findById(id)).thenReturn(Optional.of(noticeEmployee));

        // 执行
        noticeService.markAsRead(userId, noticeId);

        // 验证状态已更新
        assertEquals(NoticeStatus.VIEWED, noticeEmployee.getNoticeStatus());

        // 验证保存到数据库
        verify(noticeEmployeeRepository, times(1)).save(noticeEmployee);

        // 验证更新 Redis
        verify(cacheService, times(1)).decrementUnreadCount(userId);
        verify(cacheService, times(1)).clearAllCache(userId);
    }

    /**
     * 测试：标记所有通知为已读
     */
    @Test
    void testMarkAllAsRead_Success() {
        // 准备数据
        Integer userId = 2;
        List<NoticeEmployee> unreadNotices = Arrays.asList(noticeEmployee);

        when(noticeEmployeeRepository.findUnreadByReceiverId(userId)).thenReturn(unreadNotices);

        // 执行
        noticeService.markAllAsRead(userId);

        // 验证状态已更新
        assertEquals(NoticeStatus.VIEWED, noticeEmployee.getNoticeStatus());

        // 验证批量保存
        verify(noticeEmployeeRepository, times(1)).saveAll(unreadNotices);

        // 验证清空 Redis
        verify(cacheService, times(1)).clearUnreadCount(userId);
        verify(cacheService, times(1)).clearAllCache(userId);
    }

    /**
     * 测试：删除通知（未读通知）
     */
    @Test
    void testDeleteNotice_Unread() {
        // 准备数据
        Integer userId = 2;
        Integer noticeId = 100;

        NoticeEmployeeId id = new NoticeEmployeeId();
        id.setNoticeId(noticeId);
        id.setReceiverId(userId);

        noticeEmployee.setNoticeStatus(NoticeStatus.NOT_VIEWED);
        when(noticeEmployeeRepository.findById(id)).thenReturn(Optional.of(noticeEmployee));

        // 执行
        noticeService.deleteNotice(userId, noticeId);

        // 验证删除
        verify(noticeEmployeeRepository, times(1)).delete(noticeEmployee);

        // 验证更新未读数（因为是未读通知）
        verify(cacheService, times(1)).decrementUnreadCount(userId);
        verify(cacheService, times(1)).clearAllCache(userId);
    }

    /**
     * 测试：删除通知（已读通知）
     */
    @Test
    void testDeleteNotice_Read() {
        // 准备数据
        Integer userId = 2;
        Integer noticeId = 100;

        NoticeEmployeeId id = new NoticeEmployeeId();
        id.setNoticeId(noticeId);
        id.setReceiverId(userId);

        noticeEmployee.setNoticeStatus(NoticeStatus.VIEWED);
        when(noticeEmployeeRepository.findById(id)).thenReturn(Optional.of(noticeEmployee));

        // 执行
        noticeService.deleteNotice(userId, noticeId);

        // 验证删除
        verify(noticeEmployeeRepository, times(1)).delete(noticeEmployee);

        // 验证不更新未读数（因为是已读通知）
        verify(cacheService, never()).decrementUnreadCount(userId);

        // 验证清空缓存
        verify(cacheService, times(1)).clearAllCache(userId);
    }

    // 辅助方法
    private NoticeDTO createNoticeDTO(Integer noticeId, String content, Integer status) {
        NoticeDTO dto = new NoticeDTO();
        dto.setNoticeId(noticeId);
        dto.setContent(content);
        dto.setSenderName("张三");
        dto.setCreatedTime(LocalDateTime.now());
        dto.setStatus(status);
        return dto;
    }
}