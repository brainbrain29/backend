package com.pandora.backend.controller;

import com.pandora.backend.dto.NoticeDTO;
import com.pandora.backend.dto.NoticeStatusDTO;
import com.pandora.backend.filter.JwtAuthFilter;
import com.pandora.backend.service.NoticeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * NoticeController 单元测试
 */
@WebMvcTest(value = NoticeController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@DisplayName("Notice Controller 测试")
class NoticeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NoticeService noticeService;

    private List<NoticeDTO> testNotices;
    private NoticeStatusDTO testStatus;

    @BeforeEach
    void setUp() {
        // 准备测试通知
        NoticeDTO notice1 = new NoticeDTO();
        notice1.setNoticeId(1);
        notice1.setContent("测试通知1");
        notice1.setSenderName("张三");
        notice1.setCreatedTime(LocalDateTime.now());
        notice1.setStatus(0); // 0=未读

        NoticeDTO notice2 = new NoticeDTO();
        notice2.setNoticeId(2);
        notice2.setContent("测试通知2");
        notice2.setSenderName("李四");
        notice2.setCreatedTime(LocalDateTime.now());
        notice2.setStatus(0);

        testNotices = Arrays.asList(notice1, notice2);

        // 准备测试状态
        testStatus = new NoticeStatusDTO(true, 5);
    }

    @Test
    @DisplayName("获取未读通知 - 成功")
    void testGetUnreadNotice_Success() throws Exception {
        // Given
        when(noticeService.getUnreadNotice(1)).thenReturn(testNotices);

        // When & Then
        mockMvc.perform(get("/notices/me/unread")
                .with(user("testuser").roles("USER"))
                .requestAttr("userId", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].noticeId").value(1))
                .andExpect(jsonPath("$[0].content").value("测试通知1"))
                .andExpect(jsonPath("$[1].noticeId").value(2));
    }

    @Test
    @DisplayName("获取未读通知 - 无未读通知")
    void testGetUnreadNotice_Empty() throws Exception {
        // Given
        when(noticeService.getUnreadNotice(1)).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/notices/me/unread")
                .with(user("testuser").roles("USER"))
                .requestAttr("userId", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("获取未读通知 - 缺少 userId")
    void testGetUnreadNotice_MissingUserId() throws Exception {
        // When & Then
        mockMvc.perform(get("/notices/me/unread")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取所有通知 - 成功")
    void testGetAllNotice_Success() throws Exception {
        // Given
        when(noticeService.getAllNotice(1)).thenReturn(testNotices);

        // When & Then
        mockMvc.perform(get("/notices/me/all")
                .with(user("testuser").roles("USER"))
                .requestAttr("userId", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("获取所有通知 - 缺少 userId")
    void testGetAllNotice_MissingUserId() throws Exception {
        // When & Then
        mockMvc.perform(get("/notices/me/all")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("检查未读通知数量 - 成功")
    void testCheckNotice_Success() throws Exception {
        // Given
        when(noticeService.checkUnreadNotice(1)).thenReturn(testStatus);

        // When & Then
        mockMvc.perform(get("/notices/check")
                .with(user("testuser").roles("USER"))
                .requestAttr("userId", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(5))
                .andExpect(jsonPath("$.hasUnreadNotice").value(true));
    }

    @Test
    @DisplayName("检查未读通知数量 - 缺少 userId")
    void testCheckNotice_MissingUserId() throws Exception {
        // When & Then
        mockMvc.perform(get("/notices/check")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("标记单个通知为已读 - 成功")
    void testMarkAsRead_Success() throws Exception {
        // Given
        doNothing().when(noticeService).markAsRead(1, 100);

        // When & Then
        mockMvc.perform(put("/notices/mark-read/100")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .requestAttr("userId", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify
        verify(noticeService, times(1)).markAsRead(1, 100);
    }

    @Test
    @DisplayName("标记单个通知为已读 - 缺少 userId")
    void testMarkAsRead_MissingUserId() throws Exception {
        // When & Then
        mockMvc.perform(put("/notices/mark-read/100")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Verify - 不应该调用 Service
        verify(noticeService, never()).markAsRead(any(), any());
    }

    @Test
    @DisplayName("标记所有通知为已读 - 成功")
    void testMarkAllAsRead_Success() throws Exception {
        // Given
        doNothing().when(noticeService).markAllAsRead(1);

        // When & Then
        mockMvc.perform(put("/notices/mark-all-read")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .requestAttr("userId", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify
        verify(noticeService, times(1)).markAllAsRead(1);
    }

    @Test
    @DisplayName("标记所有通知为已读 - 缺少 userId")
    void testMarkAllAsRead_MissingUserId() throws Exception {
        // When & Then
        mockMvc.perform(put("/notices/mark-all-read")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Verify - 不应该调用 Service
        verify(noticeService, never()).markAllAsRead(any());
    }

    @Test
    @DisplayName("删除通知 - 成功")
    void testDeleteNotice_Success() throws Exception {
        // Given
        doNothing().when(noticeService).deleteNotice(1, 100);

        // When & Then
        mockMvc.perform(delete("/notices/100")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .requestAttr("userId", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Verify
        verify(noticeService, times(1)).deleteNotice(1, 100);
    }

    @Test
    @DisplayName("删除通知 - 缺少 userId")
    void testDeleteNotice_MissingUserId() throws Exception {
        // When & Then
        mockMvc.perform(delete("/notices/100")
                .with(user("testuser").roles("USER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Verify - 不应该调用 Service
        verify(noticeService, never()).deleteNotice(any(), any());
    }
}
