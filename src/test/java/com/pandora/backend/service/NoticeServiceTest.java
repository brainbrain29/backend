package com.pandora.backend.service;

import com.pandora.backend.dto.NoticeDTO;
import com.pandora.backend.dto.NoticeStatusDTO;
import com.pandora.backend.entity.*;
import com.pandora.backend.enums.NoticeStatus;
import com.pandora.backend.repository.NoticeEmployeeRepository;
import com.pandora.backend.repository.NoticeRepository;
import com.pandora.backend.util.RedisUtil;
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
import java.util.concurrent.TimeUnit;

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
    private com.pandora.backend.repository.EmployeeRepository employeeRepository;

    @Mock
    private NotificationCacheService cacheService;

    @Mock
    private NotificationPushService pushService;

    @Mock
    private RedisUtil redisUtil;

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

        // Mock Redis 锁操作，让所有测试都能成功获取锁
        // 使用 lenient() 允许某些测试不使用这些 Mock
        lenient().when(redisUtil.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        // Mock NotificationCacheService 的锁操作（这是实际使用的）
        lenient().when(cacheService.tryLock(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);

        // Mock 删除锁操作（delete 返回 Boolean）
        lenient().when(redisUtil.delete(anyString())).thenReturn(true);
        lenient().when(cacheService.releaseLock(anyString(), anyString())).thenReturn(true);
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

        // Mock 分布式锁
        when(cacheService.tryLock(anyString(), anyString(), anyLong(), any())).thenReturn(true);
        when(cacheService.releaseLock(anyString(), anyString())).thenReturn(true);

        // 执行
        noticeService.markAsRead(userId, noticeId);

        // 验证状态已更新
        assertEquals(NoticeStatus.VIEWED, noticeEmployee.getNoticeStatus());

        // 验证保存到数据库
        verify(noticeEmployeeRepository, times(1)).save(noticeEmployee);

        // 验证清空 Redis（延迟双删，会调用2次）
        verify(cacheService, times(2)).clearAllCache(userId);
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

        // 验证清空 Redis 缓存（实际调用了 2 次 clearAllCache）
        verify(cacheService, times(2)).clearAllCache(userId);
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

    /**
     * 测试：获取所有通知（包括已读和未读）
     */
    @Test
    void testGetAllNotice() {
        // 准备数据
        Integer userId = 2;
        List<NoticeEmployee> noticeList = Arrays.asList(noticeEmployee);
        
        when(noticeEmployeeRepository.findAllByReceiverId(userId)).thenReturn(noticeList);
        
        // 执行
        List<NoticeDTO> result = noticeService.getAllNotice(userId);
        
        // 验证
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100, result.get(0).getNoticeId());
        verify(noticeEmployeeRepository, times(1)).findAllByReceiverId(userId);
    }

    /**
     * 测试：创建任务分配通知（正常情况）
     */
    @Test
    void testCreateTaskAssignmentNotice_Success() {
        // 准备数据
        Task task = new Task();
        task.setTaskId(1);
        task.setTitle("完成项目报告");
        task.setAssignee(receiver);
        task.setSender(sender);
        
        when(noticeRepository.save(any(Notice.class))).thenReturn(notice);
        when(pushService.isUserOnline(receiver.getEmployeeId())).thenReturn(true);
        when(noticeEmployeeRepository.save(any(NoticeEmployee.class))).thenReturn(noticeEmployee);
        
        // 执行
        noticeService.createTaskAssignmentNotice(task);
        
        // 验证
        verify(noticeRepository, times(1)).save(any(Notice.class));
        verify(noticeEmployeeRepository, times(1)).save(any(NoticeEmployee.class));
        verify(cacheService, times(1)).incrementUnreadCount(receiver.getEmployeeId());
        verify(cacheService, times(1)).cacheRecentNotice(eq(receiver.getEmployeeId()), any(NoticeDTO.class));
        verify(pushService, times(1)).pushNotification(eq(receiver.getEmployeeId()), any(NoticeDTO.class));
    }

    /**
     * 测试：创建任务分配通知（分配者和执行者是同一人）
     */
    @Test
    void testCreateTaskAssignmentNotice_SamePerson() {
        // 准备数据
        Task task = new Task();
        task.setTaskId(1);
        task.setTitle("完成项目报告");
        task.setAssignee(sender);  // 分配者和执行者是同一人
        task.setSender(sender);
        
        // 执行
        noticeService.createTaskAssignmentNotice(task);
        
        // 验证：不应该创建通知
        verify(noticeRepository, never()).save(any(Notice.class));
        verify(noticeEmployeeRepository, never()).save(any(NoticeEmployee.class));
    }

    /**
     * 测试：创建任务分配通知（缺少必要信息）
     */
    @Test
    void testCreateTaskAssignmentNotice_MissingInfo() {
        // 准备数据：缺少 assignee
        Task task = new Task();
        task.setTaskId(1);
        task.setTitle("完成项目报告");
        task.setSender(sender);
        // task.setAssignee(null);  // 没有分配执行者
        
        // 执行
        noticeService.createTaskAssignmentNotice(task);
        
        // 验证：不应该创建通知
        verify(noticeRepository, never()).save(any(Notice.class));
    }

    /**
     * 测试：批量更新通知状态为已接收
     */
    @Test
    void testMarkAsReceived() {
        // 准备数据
        Integer userId = 2;
        List<Integer> noticeIds = Arrays.asList(100, 101);
        
        NoticeEmployeeId id1 = new NoticeEmployeeId(100, userId);
        NoticeEmployee ne1 = new NoticeEmployee();
        ne1.setId(id1);
        ne1.setNoticeStatus(NoticeStatus.NOT_RECEIVED);
        
        NoticeEmployeeId id2 = new NoticeEmployeeId(101, userId);
        NoticeEmployee ne2 = new NoticeEmployee();
        ne2.setId(id2);
        ne2.setNoticeStatus(NoticeStatus.NOT_RECEIVED);
        
        when(noticeEmployeeRepository.findById(id1)).thenReturn(Optional.of(ne1));
        when(noticeEmployeeRepository.findById(id2)).thenReturn(Optional.of(ne2));
        
        // 执行
        noticeService.markAsReceived(userId, noticeIds);
        
        // 验证
        assertEquals(NoticeStatus.NOT_VIEWED, ne1.getNoticeStatus());
        assertEquals(NoticeStatus.NOT_VIEWED, ne2.getNoticeStatus());
        verify(noticeEmployeeRepository, times(2)).save(any(NoticeEmployee.class));
    }

    /**
     * 测试：批量更新通知状态为已接收（空列表）
     */
    @Test
    void testMarkAsReceived_EmptyList() {
        // 执行
        noticeService.markAsReceived(2, null);
        noticeService.markAsReceived(2, Arrays.asList());
        
        // 验证：不应该有任何操作
        verify(noticeEmployeeRepository, never()).findById(any());
    }

    /**
     * 测试：创建任务状态更新通知
     */
    @Test
    void testCreateTaskUpdateNotice() {
        // 准备数据
        Task task = new Task();
        task.setTaskId(1);
        task.setTitle("完成项目报告");
        task.setAssignee(receiver);
        
        Employee updater = new Employee();
        updater.setEmployeeId(3);
        updater.setEmployeeName("王五");
        
        when(noticeRepository.save(any(Notice.class))).thenReturn(notice);
        when(noticeEmployeeRepository.save(any(NoticeEmployee.class))).thenReturn(noticeEmployee);
        
        // 执行
        noticeService.createTaskUpdateNotice(task, updater);
        
        // 验证
        verify(noticeRepository, times(1)).save(any(Notice.class));
        verify(noticeEmployeeRepository, times(1)).save(any(NoticeEmployee.class));
        verify(cacheService, times(1)).incrementUnreadCount(receiver.getEmployeeId());
        verify(pushService, times(1)).pushNotification(eq(receiver.getEmployeeId()), any(NoticeDTO.class));
    }

    /**
     * 测试：创建任务状态更新通知（更新者和负责人是同一人）
     */
    @Test
    void testCreateTaskUpdateNotice_SamePerson() {
        // 准备数据
        Task task = new Task();
        task.setTaskId(1);
        task.setTitle("完成项目报告");
        task.setAssignee(sender);
        
        // 执行：更新者和负责人是同一人
        noticeService.createTaskUpdateNotice(task, sender);
        
        // 验证：不应该创建通知
        verify(noticeRepository, never()).save(any(Notice.class));
    }

    /**
     * 测试：创建公司重要事项通知
     */
    @Test
    void testCreateCompanyMatterNotice() {
        // 准备数据
        Employee employee1 = new Employee();
        employee1.setEmployeeId(2);
        employee1.setEmployeeName("李四");
        
        Employee employee2 = new Employee();
        employee2.setEmployeeId(3);
        employee2.setEmployeeName("王五");
        
        List<Employee> allEmployees = Arrays.asList(sender, employee1, employee2);
        
        when(employeeRepository.findAll()).thenReturn(allEmployees);
        when(noticeEmployeeRepository.save(any(NoticeEmployee.class))).thenReturn(noticeEmployee);
        
        // 执行
        noticeService.createCompanyMatterNotice(notice, 1); // 传入测试用的matterId
        
        // 验证：应该为除发送者外的所有员工创建通知（2个）
        verify(noticeEmployeeRepository, times(2)).save(any(NoticeEmployee.class));
        verify(cacheService, times(2)).incrementUnreadCount(anyInt());
        verify(pushService, times(2)).pushNotification(anyInt(), any(NoticeDTO.class));
    }

    /**
     * 测试：创建公司重要任务通知
     */
    @Test
    void testCreateImportantTaskNotice() {
        // 准备数据
        Task importantTask = new Task();
        importantTask.setTaskId(1);
        importantTask.setTitle("重要任务：完成年度报告");
        importantTask.setSender(sender);
        
        Employee employee1 = new Employee();
        employee1.setEmployeeId(2);
        employee1.setEmployeeName("李四");
        
        Employee employee2 = new Employee();
        employee2.setEmployeeId(3);
        employee2.setEmployeeName("王五");
        
        List<Employee> allEmployees = Arrays.asList(sender, employee1, employee2);
        
        when(employeeRepository.findAll()).thenReturn(allEmployees);
        when(noticeRepository.save(any(Notice.class))).thenReturn(notice);
        when(noticeEmployeeRepository.save(any(NoticeEmployee.class))).thenReturn(noticeEmployee);
        
        // 执行
        noticeService.createImportantTaskNotice(importantTask);
        
        // 验证：应该为除发送者外的所有员工创建通知（2个）
        verify(noticeRepository, times(2)).save(any(Notice.class));
        verify(noticeEmployeeRepository, times(2)).save(any(NoticeEmployee.class));
        verify(cacheService, times(2)).incrementUnreadCount(anyInt());
        verify(pushService, times(2)).pushNotification(anyInt(), any(NoticeDTO.class));
    }

    // 辅助方法
    private NoticeDTO createNoticeDTO(Integer noticeId, String content, Integer status) {
        NoticeDTO dto = new NoticeDTO();
        dto.setNoticeId(noticeId);
        dto.setTitle("新任务派发"); // 默认使用新任务派发类型
        dto.setContent(content);
        dto.setSenderName("张三");
        dto.setCreatedTime(LocalDateTime.now());
        dto.setStatus(com.pandora.backend.enums.NoticeStatus.fromCode(status).getDesc());
        dto.setRelatedId(100); // 默认关联ID
        return dto;
    }
}