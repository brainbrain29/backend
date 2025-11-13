package com.pandora.backend.service;

import com.pandora.backend.dto.LogDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.Log;
import com.pandora.backend.entity.Task;
import com.pandora.backend.enums.Emoji;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.repository.LogAttachmentRepository;
import com.pandora.backend.repository.LogRepository;
import com.pandora.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * LogService Unit Test
 * Tests business logic without real database dependency
 */
@ExtendWith(MockitoExtension.class)
class LogServiceTest {

    @Mock
    private LogRepository logRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private LogAttachmentRepository logAttachmentRepository;

    @InjectMocks
    private LogService logService;

    private Employee employee;
    private Task task;
    private Log log;
    private LogDTO logDTO;

    @BeforeEach
    void setUp() {
        // Initialize test data
        employee = new Employee();
        employee.setEmployeeId(1);
        employee.setEmployeeName("张三");

        task = new Task();
        task.setTaskId(100);
        task.setTitle("完成项目报告");

        log = new Log();
        log.setLogId(1);
        log.setEmployee(employee);
        log.setTask(task);
        log.setContent("今天完成了项目报告的初稿");
        log.setEmoji((byte) Emoji.HAPPY.getCode());
        log.setAttachment(null);
        log.setEmployeeLocation("北京");
        log.setCreatedTime(LocalDateTime.now());

        logDTO = new LogDTO();
        logDTO.setEmployeeId(1);
        logDTO.setTaskId(100);
        logDTO.setContent("今天完成了项目报告的初稿");
        logDTO.setEmoji("开心");
        logDTO.setEmployeeLocation("北京");
    }

    /**
     * Test: Create log successfully
     */
    @Test
    void testCreateLog_Success() {
        // Mock repository responses
        when(employeeRepository.findById(1)).thenReturn(Optional.of(employee));
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(logRepository.save(any(Log.class))).thenReturn(log);

        // Execute
        Log result = logService.createLog(logDTO);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.getLogId());
        assertEquals("今天完成了项目报告的初稿", result.getContent());
        assertEquals(employee, result.getEmployee());
        assertEquals(task, result.getTask());

        // Verify repository calls
        verify(employeeRepository, times(1)).findById(1);
        verify(taskRepository, times(1)).findById(100);
        verify(logRepository, times(1)).save(any(Log.class));
    }

    /**
     * Test: Create log without task (personal log)
     */
    @Test
    void testCreateLog_WithoutTask() {
        // Prepare data without task
        logDTO.setTaskId(null);
        log.setTask(null);

        // Mock repository responses
        when(employeeRepository.findById(1)).thenReturn(Optional.of(employee));
        when(logRepository.save(any(Log.class))).thenReturn(log);

        // Execute
        Log result = logService.createLog(logDTO);

        // Verify
        assertNotNull(result);
        assertNull(result.getTask());
        verify(taskRepository, never()).findById(any());
        verify(logRepository, times(1)).save(any(Log.class));
    }

    /**
     * Test: Create log with non-existent employee should throw exception
     */
    @Test
    void testCreateLog_EmployeeNotFound() {
        // Mock employee not found
        when(employeeRepository.findById(1)).thenReturn(Optional.empty());

        // Execute and verify exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            logService.createLog(logDTO);
        });

        assertTrue(exception.getMessage().contains("Employee not found"));
        verify(logRepository, never()).save(any(Log.class));
    }

    /**
     * Test: Create log with non-existent task should throw exception
     */
    @Test
    void testCreateLog_TaskNotFound() {
        // Mock employee exists but task not found
        when(employeeRepository.findById(1)).thenReturn(Optional.of(employee));
        when(taskRepository.findById(100)).thenReturn(Optional.empty());

        // Execute and verify exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            logService.createLog(logDTO);
        });

        assertTrue(exception.getMessage().contains("Task not found"));
        verify(logRepository, never()).save(any(Log.class));
    }

    /**
     * Test: Create log with default emoji
     */
    @Test
    void testCreateLog_DefaultEmoji() {
        // Prepare data without emoji
        logDTO.setEmoji(null);

        // Mock repository responses
        when(employeeRepository.findById(1)).thenReturn(Optional.of(employee));
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(logRepository.save(any(Log.class))).thenAnswer(invocation -> {
            Log savedLog = invocation.getArgument(0);
            assertEquals((byte) Emoji.PEACE.getCode(), savedLog.getEmoji());
            return savedLog;
        });

        // Execute
        Log result = logService.createLog(logDTO);

        // Verify
        verify(logRepository, times(1)).save(any(Log.class));
    }

    /**
     * Test: Get all logs
     */
    @Test
    void testGetAllLogs() {
        // Prepare test data
        List<Log> logs = Arrays.asList(log);

        // Mock repository response
        when(logRepository.findAll()).thenReturn(logs);

        // Execute
        List<LogDTO> result = logService.getAllLogs();

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("张三", result.get(0).getEmployeeName());
        assertEquals("完成项目报告", result.get(0).getTaskName());
        verify(logRepository, times(1)).findAll();
    }

    /**
     * Test: Get log by ID successfully
     */
    @Test
    void testGetLogById_Success() {
        // Mock repository response
        when(logRepository.findById(1)).thenReturn(Optional.of(log));

        // Execute
        LogDTO result = logService.getLogById(1);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.getLogId());
        assertEquals("今天完成了项目报告的初稿", result.getContent());
        assertEquals("张三", result.getEmployeeName());
        verify(logRepository, times(1)).findById(1);
    }

    /**
     * Test: Get non-existent log should throw exception
     */
    @Test
    void testGetLogById_LogNotFound() {
        // Mock log not found
        when(logRepository.findById(999)).thenReturn(Optional.empty());

        // Execute and verify exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            logService.getLogById(999);
        });

        assertTrue(exception.getMessage().contains("Log not found"));
    }

    /**
     * Test: Get logs by task ID
     */
    @Test
    void testGetLogsByTask() {
        // Prepare test data
        List<Log> logs = Arrays.asList(log);

        // Mock repository response
        when(logRepository.findByTask_TaskId(100)).thenReturn(logs);

        // Execute
        List<LogDTO> result = logService.getLogsByTask(100);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("完成项目报告", result.get(0).getTaskName());
        verify(logRepository, times(1)).findByTask_TaskId(100);
    }

    /**
     * Test: Get logs by date
     */
    @Test
    void testGetLogsByDate() {
        // Prepare test data
        LocalDateTime dateTime = LocalDateTime.of(2025, 11, 11, 10, 0);
        LocalDateTime startOfDay = dateTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = dateTime.toLocalDate().atTime(LocalTime.MAX);
        List<Log> logs = Arrays.asList(log);

        // Mock repository response
        when(logRepository.findByEmployeeEmployeeIdAndCreatedTimeBetween(1, startOfDay, endOfDay))
                .thenReturn(logs);

        // Execute
        List<LogDTO> result = logService.getLogsByDate(1, dateTime);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(logRepository, times(1))
                .findByEmployeeEmployeeIdAndCreatedTimeBetween(1, startOfDay, endOfDay);
    }

    /**
     * Test: Search logs with keyword
     */
    @Test
    void testSearchLogs_WithKeyword() {
        // Prepare test data
        List<Log> logs = Arrays.asList(log);

        // Mock repository response
        when(logRepository.searchByKeyword("项目")).thenReturn(logs);

        // Execute
        List<LogDTO> result = logService.searchLogs("项目");

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(logRepository, times(1)).searchByKeyword("项目");
    }

    /**
     * Test: Search logs with null keyword
     */
    @Test
    void testSearchLogs_NullKeyword() {
        // Execute
        List<LogDTO> result = logService.searchLogs(null);

        // Verify
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(logRepository, never()).searchByKeyword(any());
    }

    /**
     * Test: Search logs with empty keyword
     */
    @Test
    void testSearchLogs_EmptyKeyword() {
        // Execute
        List<LogDTO> result = logService.searchLogs("   ");

        // Verify
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(logRepository, never()).searchByKeyword(any());
    }

    /**
     * Test: Update log successfully
     */
    @Test
    void testUpdateLog_Success() {
        // Prepare update data
        LogDTO updateDTO = new LogDTO();
        updateDTO.setContent("更新后的内容");
        updateDTO.setEmoji("难过");
        updateDTO.setEmployeeLocation("上海");
        updateDTO.setTaskId(100);

        // Mock repository responses
        when(logRepository.findById(1)).thenReturn(Optional.of(log));
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(logRepository.save(any(Log.class))).thenReturn(log);

        // Execute
        LogDTO result = logService.updateLog(1, updateDTO);

        // Verify
        assertNotNull(result);
        verify(logRepository, times(1)).findById(1);
        verify(logRepository, times(1)).save(any(Log.class));
    }

    /**
     * Test: Update non-existent log should throw exception
     */
    @Test
    void testUpdateLog_LogNotFound() {
        // Mock log not found
        when(logRepository.findById(999)).thenReturn(Optional.empty());

        // Execute and verify exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            logService.updateLog(999, logDTO);
        });

        assertTrue(exception.getMessage().contains("Log not found"));
        verify(logRepository, never()).save(any(Log.class));
    }

    /**
     * Test: Update log and remove task association
     */
    @Test
    void testUpdateLog_RemoveTask() {
        // Prepare update data without task
        LogDTO updateDTO = new LogDTO();
        updateDTO.setContent("更新后的内容");
        updateDTO.setTaskId(null);

        // Mock repository responses
        when(logRepository.findById(1)).thenReturn(Optional.of(log));
        when(logRepository.save(any(Log.class))).thenAnswer(invocation -> {
            Log savedLog = invocation.getArgument(0);
            assertNull(savedLog.getTask());
            return savedLog;
        });

        // Execute
        LogDTO result = logService.updateLog(1, updateDTO);

        // Verify
        verify(logRepository, times(1)).save(any(Log.class));
    }

    /**
     * Test: Delete log successfully
     */
    @Test
    void testDeleteLog_Success() {
        // Mock log exists
        when(logRepository.existsById(1)).thenReturn(true);
        doNothing().when(logRepository).deleteById(1);

        // Execute
        logService.deleteLog(1);

        // Verify
        verify(logRepository, times(1)).existsById(1);
        verify(logRepository, times(1)).deleteById(1);
    }

    /**
     * Test: Delete non-existent log should throw exception
     */
    @Test
    void testDeleteLog_LogNotFound() {
        // Mock log not found
        when(logRepository.existsById(999)).thenReturn(false);

        // Execute and verify exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            logService.deleteLog(999);
        });

        assertTrue(exception.getMessage().contains("Log not found"));
        verify(logRepository, never()).deleteById(any());
    }

    /**
     * Test: Query logs in week
     */
    @Test
    void testQueryLogsInWeek() {
        // Prepare test data
        LocalDate startDate = LocalDate.of(2025, 11, 10);
        LocalDate endDate = LocalDate.of(2025, 11, 16);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<Log> logs = Arrays.asList(log);

        // Mock repository response
        when(logRepository.findByEmployeeEmployeeIdAndCreatedTimeBetween(1, startDateTime, endDateTime))
                .thenReturn(logs);

        // Execute
        List<LogDTO> result = logService.queryLogsInWeek(1, startDate, endDate);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(logRepository, times(1))
                .findByEmployeeEmployeeIdAndCreatedTimeBetween(1, startDateTime, endDateTime);
    }

    /**
     * Test: Query logs in week with auto end date
     */
    @Test
    void testQueryLogsInWeek_AutoEndDate() {
        // Prepare test data
        LocalDate startDate = LocalDate.of(2025, 11, 10);
        LocalDate expectedEndDate = startDate.plusDays(6);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = expectedEndDate.atTime(23, 59, 59);
        List<Log> logs = Arrays.asList(log);

        // Mock repository response
        when(logRepository.findByEmployeeEmployeeIdAndCreatedTimeBetween(1, startDateTime, endDateTime))
                .thenReturn(logs);

        // Execute (without end date)
        List<LogDTO> result = logService.queryLogsInWeek(1, startDate, null);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * Test: Query logs in month
     */
    @Test
    void testQueryLogsInMonth() {
        // Prepare test data
        LocalDate startDate = LocalDate.of(2025, 11, 1);
        LocalDate endDate = LocalDate.of(2025, 11, 30);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
        List<Log> logs = Arrays.asList(log);

        // Mock repository response
        when(logRepository.findByEmployeeEmployeeIdAndCreatedTimeBetween(1, startDateTime, endDateTime))
                .thenReturn(logs);

        // Execute
        List<LogDTO> result = logService.queryLogsInMonth(1, startDate, endDate);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(logRepository, times(1))
                .findByEmployeeEmployeeIdAndCreatedTimeBetween(1, startDateTime, endDateTime);
    }

    /**
     * Test: Query logs in month with auto end date
     */
    @Test
    void testQueryLogsInMonth_AutoEndDate() {
        // Prepare test data
        LocalDate startDate = LocalDate.of(2025, 11, 15);
        LocalDate expectedEndDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = expectedEndDate.atTime(23, 59, 59);
        List<Log> logs = Arrays.asList(log);

        // Mock repository response
        when(logRepository.findByEmployeeEmployeeIdAndCreatedTimeBetween(1, startDateTime, endDateTime))
                .thenReturn(logs);

        // Execute (without end date)
        List<LogDTO> result = logService.queryLogsInMonth(1, startDate, null);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    /**
     * Test: Get detail log
     */
    @Test
    void testGetDetailLog() {
        // Mock repository response
        when(logRepository.findById(1)).thenReturn(Optional.of(log));

        // Execute
        LogDTO result = logService.getDetailLog(1);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.getLogId());
        assertEquals("今天完成了项目报告的初稿", result.getContent());
        verify(logRepository, times(1)).findById(1);
    }

    /**
     * Test: Create log with attachments (mock file upload scenario)
     */
    @Test
    void testCreateLogWithAttachments_NoFiles() {
        // Mock repository responses
        when(employeeRepository.findById(1)).thenReturn(Optional.of(employee));
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(logRepository.save(any(Log.class))).thenReturn(log);

        // Execute with no files
        Log result = logService.createLogWithAttachments(logDTO, null, 1);

        // Verify
        assertNotNull(result);
        verify(logRepository, times(1)).save(any(Log.class));
        verify(logAttachmentRepository, never()).save(any());
    }

    /**
     * Test: Create log with empty files array
     */
    @Test
    void testCreateLogWithAttachments_EmptyFiles() {
        // Mock repository responses
        when(employeeRepository.findById(1)).thenReturn(Optional.of(employee));
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(logRepository.save(any(Log.class))).thenReturn(log);

        // Execute with empty files array
        MultipartFile[] emptyFiles = new MultipartFile[0];
        Log result = logService.createLogWithAttachments(logDTO, emptyFiles, 1);

        // Verify
        assertNotNull(result);
        verify(logRepository, times(1)).save(any(Log.class));
        verify(logAttachmentRepository, never()).save(any());
    }
}
