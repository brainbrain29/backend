package com.pandora.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pandora.backend.dto.LogDTO;
import com.pandora.backend.entity.Log;
import com.pandora.backend.service.LogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * LogController Integration Test
 * Tests HTTP endpoints with mocked service layer
 */
@WebMvcTest(controllers = LogController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        })
@AutoConfigureMockMvc(addFilters = false)
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LogService logService;

    @MockBean
    private com.pandora.backend.util.JwtUtil jwtUtil;

    @MockBean
    private com.pandora.backend.filter.JwtAuthFilter jwtAuthFilter;

    private ObjectMapper objectMapper;
    private LogDTO logDTO;
    private Log log;

    @BeforeEach
    void setUp() {
        // Initialize ObjectMapper with Java 8 Time support
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Initialize test data
        log = new Log();
        log.setLogId(1);
        log.setContent("今天完成了项目报告的初稿");
        log.setCreatedTime(LocalDateTime.now());

        logDTO = new LogDTO();
        logDTO.setLogId(1);
        logDTO.setEmployeeId(1);
        logDTO.setEmployeeName("张三");
        logDTO.setTaskId(100);
        logDTO.setTaskName("完成项目报告");
        logDTO.setContent("今天完成了项目报告的初稿");
        logDTO.setEmoji("开心");
        logDTO.setEmployeeLocation("北京");
        logDTO.setCreatedTime(LocalDateTime.now());
    }

    /**
     * Test: Create log successfully (without files)
     */
    @Test
    void testCreateLog_Success() throws Exception {
        // Mock service response
        when(logService.createLogWithAttachments(any(LogDTO.class), any(), eq(1)))
                .thenReturn(log);
        when(logService.getLogById(1)).thenReturn(logDTO);

        // Execute and verify
        mockMvc.perform(multipart("/logs")
                .param("content", "今天完成了项目报告的初稿")
                .param("mood", "开心")
                .param("taskId", "100")
                .param("employeeLocation", "北京")
                .requestAttr("userId", 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.logId").value(1))
                .andExpect(jsonPath("$.content").value("今天完成了项目报告的初稿"))
                .andExpect(jsonPath("$.employeeName").value("张三"));

        verify(logService, times(1)).createLogWithAttachments(any(LogDTO.class), any(), eq(1));
        verify(logService, times(1)).getLogById(1);
    }

    /**
     * Test: Create log with file attachments
     */
    @Test
    void testCreateLog_WithFiles() throws Exception {
        // Prepare mock file
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "Test content".getBytes()
        );

        // Mock service response
        when(logService.createLogWithAttachments(any(LogDTO.class), any(), eq(1)))
                .thenReturn(log);
        when(logService.getLogById(1)).thenReturn(logDTO);

        // Execute and verify
        mockMvc.perform(multipart("/logs")
                .file(file)
                .param("content", "今天完成了项目报告的初稿")
                .param("mood", "开心")
                .requestAttr("userId", 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.logId").value(1));

        verify(logService, times(1)).createLogWithAttachments(any(LogDTO.class), any(), eq(1));
    }

    /**
     * Test: Create log without authentication
     */
    @Test
    void testCreateLog_Unauthorized() throws Exception {
        // Execute without userId attribute
        mockMvc.perform(multipart("/logs")
                .param("content", "今天完成了项目报告的初稿"))
                .andExpect(status().isUnauthorized());

        verify(logService, never()).createLogWithAttachments(any(), any(), any());
    }

    /**
     * Test: Create log with service exception
     */
    @Test
    void testCreateLog_ServiceException() throws Exception {
        // Mock service to throw exception
        when(logService.createLogWithAttachments(any(LogDTO.class), any(), eq(1)))
                .thenThrow(new RuntimeException("创建失败"));

        // Execute and verify
        mockMvc.perform(multipart("/logs")
                .param("content", "今天完成了项目报告的初稿")
                .requestAttr("userId", 1))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test: Get all logs
     */
    @Test
    void testGetAllLogs() throws Exception {
        // Prepare test data
        List<LogDTO> logs = Arrays.asList(logDTO);

        // Mock service response
        when(logService.getAllLogs()).thenReturn(logs);

        // Execute and verify
        mockMvc.perform(get("/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].logId").value(1))
                .andExpect(jsonPath("$[0].content").value("今天完成了项目报告的初稿"));

        verify(logService, times(1)).getAllLogs();
    }

    /**
     * Test: Search logs with keyword
     */
    @Test
    void testSearchLogs() throws Exception {
        // Prepare test data
        List<LogDTO> logs = Arrays.asList(logDTO);

        // Mock service response
        when(logService.searchLogs("项目")).thenReturn(logs);

        // Execute and verify
        mockMvc.perform(get("/logs/search")
                .param("keyword", "项目"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("今天完成了项目报告的初稿"));

        verify(logService, times(1)).searchLogs("项目");
    }

    /**
     * Test: Get log by ID
     */
    @Test
    void testGetLogById() throws Exception {
        // Mock service response
        when(logService.getLogById(1)).thenReturn(logDTO);

        // Execute and verify
        mockMvc.perform(get("/logs/1")
                .requestAttr("userId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logId").value(1))
                .andExpect(jsonPath("$.content").value("今天完成了项目报告的初稿"));

        verify(logService, times(1)).getLogById(1);
    }

    /**
     * Test: Get log by ID without authentication
     */
    @Test
    void testGetLogById_Unauthorized() throws Exception {
        // Execute without userId attribute
        mockMvc.perform(get("/logs/1"))
                .andExpect(status().isUnauthorized());

        verify(logService, never()).getLogById(any());
    }

    /**
     * Test: Update log successfully
     */
    @Test
    void testUpdateLog_Success() throws Exception {
        // Prepare update data
        LogDTO updateDTO = new LogDTO();
        updateDTO.setContent("更新后的内容");
        updateDTO.setEmoji("难过");

        // Mock service response
        when(logService.updateLog(eq(1), any(LogDTO.class))).thenReturn(logDTO);

        // Execute and verify
        mockMvc.perform(put("/logs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logId").value(1));

        verify(logService, times(1)).updateLog(eq(1), any(LogDTO.class));
    }

    /**
     * Test: Delete log successfully
     */
    @Test
    void testDeleteLog_Success() throws Exception {
        // Mock service
        doNothing().when(logService).deleteLog(1);

        // Execute and verify
        mockMvc.perform(delete("/logs/1"))
                .andExpect(status().isNoContent());

        verify(logService, times(1)).deleteLog(1);
    }

    /**
     * Test: Get logs by date
     */
    @Test
    void testGetLogsByDate() throws Exception {
        // Prepare test data
        List<LogDTO> logs = Arrays.asList(logDTO);

        // Mock service response
        when(logService.getLogsByDate(eq(1), any(LocalDateTime.class))).thenReturn(logs);

        // Execute and verify
        mockMvc.perform(get("/logs/byDate")
                .param("datetime", "2025-11-11")
                .requestAttr("userId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].logId").value(1));

        verify(logService, times(1)).getLogsByDate(eq(1), any(LocalDateTime.class));
    }

    /**
     * Test: Get logs by date without authentication
     */
    @Test
    void testGetLogsByDate_Unauthorized() throws Exception {
        // Execute without userId attribute
        mockMvc.perform(get("/logs/byDate")
                .param("datetime", "2025-11-11"))
                .andExpect(status().isUnauthorized());

        verify(logService, never()).getLogsByDate(any(), any());
    }

    /**
     * Test: Get detail log
     */
    @Test
    void testGetDetailLog() throws Exception {
        // Mock service response
        when(logService.getDetailLog(1)).thenReturn(logDTO);

        // Execute and verify
        mockMvc.perform(get("/logs/1/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logId").value(1))
                .andExpect(jsonPath("$.content").value("今天完成了项目报告的初稿"));

        verify(logService, times(1)).getDetailLog(1);
    }

    /**
     * Test: Query logs in week with both dates
     */
    @Test
    void testQueryLogsInWeek_WithBothDates() throws Exception {
        // Prepare test data
        List<LogDTO> logs = Arrays.asList(logDTO);

        // Mock service response
        when(logService.queryLogsInWeek(eq(1), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(logs);

        // Execute and verify
        mockMvc.perform(get("/logs/week")
                .param("startDate", "2025-11-10")
                .param("endDate", "2025-11-16")
                .requestAttr("userId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].logId").value(1));

        verify(logService, times(1)).queryLogsInWeek(eq(1), any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * Test: Query logs in week with only start date
     */
    @Test
    void testQueryLogsInWeek_OnlyStartDate() throws Exception {
        // Prepare test data
        List<LogDTO> logs = Arrays.asList(logDTO);

        // Mock service response
        when(logService.queryLogsInWeek(eq(1), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(logs);

        // Execute and verify
        mockMvc.perform(get("/logs/week")
                .param("startDate", "2025-11-10")
                .requestAttr("userId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].logId").value(1));

        verify(logService, times(1)).queryLogsInWeek(eq(1), any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * Test: Query logs in week without authentication
     */
    @Test
    void testQueryLogsInWeek_Unauthorized() throws Exception {
        // Execute without userId attribute
        mockMvc.perform(get("/logs/week")
                .param("startDate", "2025-11-10"))
                .andExpect(status().isUnauthorized());

        verify(logService, never()).queryLogsInWeek(any(), any(), any());
    }

    /**
     * Test: Query logs in month with both dates
     */
    @Test
    void testQueryLogsInMonth_WithBothDates() throws Exception {
        // Prepare test data
        List<LogDTO> logs = Arrays.asList(logDTO);

        // Mock service response
        when(logService.queryLogsInMonth(eq(1), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(logs);

        // Execute and verify
        mockMvc.perform(get("/logs/month")
                .param("startDate", "2025-11-01")
                .param("endDate", "2025-11-30")
                .requestAttr("userId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].logId").value(1));

        verify(logService, times(1)).queryLogsInMonth(eq(1), any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * Test: Query logs in month with only start date
     */
    @Test
    void testQueryLogsInMonth_OnlyStartDate() throws Exception {
        // Prepare test data
        List<LogDTO> logs = Arrays.asList(logDTO);

        // Mock service response
        when(logService.queryLogsInMonth(eq(1), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(logs);

        // Execute and verify
        mockMvc.perform(get("/logs/month")
                .param("startDate", "2025-11-01")
                .requestAttr("userId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].logId").value(1));

        verify(logService, times(1)).queryLogsInMonth(eq(1), any(LocalDate.class), any(LocalDate.class));
    }

    /**
     * Test: Query logs in month without authentication
     */
    @Test
    void testQueryLogsInMonth_Unauthorized() throws Exception {
        // Execute without userId attribute
        mockMvc.perform(get("/logs/month")
                .param("startDate", "2025-11-01"))
                .andExpect(status().isUnauthorized());

        verify(logService, never()).queryLogsInMonth(any(), any(), any());
    }

    /**
     * Test: Create log with missing content parameter
     */
    @Test
    void testCreateLog_MissingContent() throws Exception {
        // Execute without content parameter
        mockMvc.perform(multipart("/logs")
                .param("mood", "开心")
                .requestAttr("userId", 1))
                .andExpect(status().isBadRequest());

        verify(logService, never()).createLogWithAttachments(any(), any(), any());
    }

    /**
     * Test: Update log with invalid JSON
     */
    @Test
    void testUpdateLog_InvalidJson() throws Exception {
        // Execute with invalid JSON
        mockMvc.perform(put("/logs/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        verify(logService, never()).updateLog(any(), any());
    }

    /**
     * Test: Get logs by invalid date format
     */
    @Test
    void testGetLogsByDate_InvalidDateFormat() throws Exception {
        // Execute with invalid date format
        mockMvc.perform(get("/logs/byDate")
                .param("datetime", "invalid-date")
                .requestAttr("userId", 1))
                .andExpect(status().isBadRequest());

        verify(logService, never()).getLogsByDate(any(), any());
    }

    /**
     * Test: Create log with exception
     */
    @Test
    void testCreateLog_Exception() throws Exception {
        when(logService.createLog(any(LogDTO.class)))
                .thenThrow(new RuntimeException("创建失败"));

        mockMvc.perform(post("/logs")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(logDTO)))
                .andExpect(status().is4xxClientError());
    }

    /**
     * Test: Get log by ID with exception
     */
    @Test
    void testGetLogById_Exception() throws Exception {
        when(logService.getLogById(999))
                .thenThrow(new RuntimeException("Log not found"));

        mockMvc.perform(get("/logs/999"))
                .andExpect(status().is4xxClientError());
    }
}
