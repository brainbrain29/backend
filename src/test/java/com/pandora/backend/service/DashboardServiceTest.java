package com.pandora.backend.service;

import com.pandora.backend.dto.HomepageDashboardDTO;
import com.pandora.backend.entity.Department;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.ImportantMatter;
import com.pandora.backend.entity.ImportantTask;
import com.pandora.backend.entity.Log;
import com.pandora.backend.entity.Task;
import com.pandora.backend.enums.Priority;
import com.pandora.backend.enums.Status;
import com.pandora.backend.repository.ImportantMatterRepository;
import com.pandora.backend.repository.ImportantTaskRepository;
import com.pandora.backend.repository.LogRepository;
import com.pandora.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DashboardService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Dashboard Service 测试")
class DashboardServiceTest {

    @Mock
    private ImportantMatterRepository importantMatterRepository;

    @Mock
    private ImportantTaskRepository importantTaskRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private LogRepository logRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private ImportantMatter testMatter;
    private ImportantTask testImportantTask;
    private Task testTask;
    private Log testLog;
    private Employee testEmployee;
    private Department testDept;

    @BeforeEach
    void setUp() {
        // 准备测试部门
        testDept = new Department();
        testDept.setOrgId(1);
        testDept.setOrgName("研发部");

        // 准备测试员工
        testEmployee = new Employee();
        testEmployee.setEmployeeId(1);
        testEmployee.setEmployeeName("张三");

        // 准备测试重要事项
        testMatter = new ImportantMatter();
        testMatter.setMatterId(1);
        testMatter.setTitle("重要通知");
        testMatter.setContent("这是一条重要通知内容");
        testMatter.setDepartment(testDept);
        testMatter.setPublishTime(LocalDateTime.now());

        // 准备测试重要任务
        testImportantTask = new ImportantTask();
        testImportantTask.setTaskId(1);
        testImportantTask.setTaskContent("完成项目开发");
        testImportantTask.setDeadline(LocalDateTime.now().plusDays(7));
        testImportantTask.setSerialNum((byte) 1);
        testImportantTask.setEmployee(testEmployee);
        testImportantTask.setTaskStatus((byte) 1); // 进行中
        testImportantTask.setTaskPriority((byte) 2); // 高优先级

        // 准备测试个人任务
        testTask = new Task();
        testTask.setTaskId(1);
        testTask.setTitle("开发新功能");
        testTask.setTaskPriority((byte) 2); // 中优先级
        testTask.setTaskStatus((byte) 1); // 未完成
        testTask.setAssignee(testEmployee);
        testTask.setEndTime(LocalDateTime.now().plusDays(3));

        // 准备测试日志
        testLog = new Log();
        testLog.setLogId(1);
        testLog.setContent("今日完成了功能开发和代码审查");
        testLog.setCreatedTime(LocalDateTime.now());
    }

    @Test
    @DisplayName("获取仪表板数据 - 成功")
    void testGetDashboardData_Success() {
        // Given
        Integer userId = 1;
        List<ImportantMatter> matters = Arrays.asList(testMatter);
        List<ImportantTask> importantTasks = Arrays.asList(testImportantTask);
        List<Task> tasks = Arrays.asList(testTask);
        List<Log> logs = Arrays.asList(testLog);

        when(importantMatterRepository.findTopMatters(any(Pageable.class))).thenReturn(matters);
        when(importantTaskRepository.findTopTasks(any(Pageable.class))).thenReturn(importantTasks);
        when(taskRepository.findTop10PersonalTasks(eq(userId), any(Pageable.class))).thenReturn(tasks);
        when(logRepository.findTodayLogsByEmployeeId(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(logs);

        // When
        HomepageDashboardDTO result = dashboardService.getDashboardData(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCompanyNotices()).hasSize(1);
        assertThat(result.getCompanyNotices().get(0).getTitle()).isEqualTo("重要通知");
        assertThat(result.getCompanyNotices().get(0).getDepartmentName()).isEqualTo("研发部");

        assertThat(result.getCompanyTasks()).hasSize(1);
        assertThat(result.getCompanyTasks().get(0).getTaskContent()).isEqualTo("完成项目开发");
        assertThat(result.getCompanyTasks().get(0).getTaskStatus()).isEqualTo("进行中");
        assertThat(result.getCompanyTasks().get(0).getTaskPriority()).isEqualTo("高");

        assertThat(result.getPersonalTasks()).hasSize(1);
        assertThat(result.getPersonalTasks().get(0).getTitle()).isEqualTo("开发新功能");
        assertThat(result.getPersonalTasks().get(0).getTaskStatus()).isEqualTo("未完成");
        assertThat(result.getPersonalTasks().get(0).getTaskPriority()).isEqualTo("中");

        assertThat(result.getTodayLogs()).hasSize(1);
        assertThat(result.getTodayLogs().get(0).getContentSummary()).contains("今日完成了功能开发");

        // Verify
        verify(importantMatterRepository).findTopMatters(any(Pageable.class));
        verify(importantTaskRepository).findTopTasks(any(Pageable.class));
        verify(taskRepository).findTop10PersonalTasks(eq(userId), any(Pageable.class));
        verify(logRepository).findTodayLogsByEmployeeId(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("获取仪表板数据 - 空数据")
    void testGetDashboardData_EmptyData() {
        // Given
        Integer userId = 1;
        when(importantMatterRepository.findTopMatters(any(Pageable.class))).thenReturn(Collections.emptyList());
        when(importantTaskRepository.findTopTasks(any(Pageable.class))).thenReturn(Collections.emptyList());
        when(taskRepository.findTop10PersonalTasks(eq(userId), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(logRepository.findTodayLogsByEmployeeId(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        HomepageDashboardDTO result = dashboardService.getDashboardData(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCompanyNotices()).isEmpty();
        assertThat(result.getCompanyTasks()).isEmpty();
        assertThat(result.getPersonalTasks()).isEmpty();
        assertThat(result.getTodayLogs()).isEmpty();
    }

    @Test
    @DisplayName("获取仪表板数据 - 日志内容截断")
    void testGetDashboardData_LogContentTruncation() {
        // Given
        Integer userId = 1;
        Log longLog = new Log();
        longLog.setLogId(1);
        longLog.setContent("这是一条非常长的日志内容，超过了50个字符的限制，应该被截断并添加省略号以便在仪表板上显示时更加简洁明了");
        longLog.setCreatedTime(LocalDateTime.now());

        when(importantMatterRepository.findTopMatters(any(Pageable.class))).thenReturn(Collections.emptyList());
        when(importantTaskRepository.findTopTasks(any(Pageable.class))).thenReturn(Collections.emptyList());
        when(taskRepository.findTop10PersonalTasks(eq(userId), any(Pageable.class))).thenReturn(Collections.emptyList());
        when(logRepository.findTodayLogsByEmployeeId(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(longLog));

        // When
        HomepageDashboardDTO result = dashboardService.getDashboardData(userId);

        // Then
        assertThat(result.getTodayLogs()).hasSize(1);
        assertThat(result.getTodayLogs().get(0).getContentSummary()).hasSize(53); // 50 + "..."
        assertThat(result.getTodayLogs().get(0).getContentSummary()).endsWith("...");
    }

    @Test
    @DisplayName("获取仪表板数据 - 验证任务状态转换")
    void testGetDashboardData_TaskStatusConversion() {
        // Given
        Integer userId = 1;
        Task pendingTask = new Task();
        pendingTask.setTaskId(1);
        pendingTask.setTitle("未完成任务");
        pendingTask.setTaskStatus((byte) 1);
        pendingTask.setTaskPriority((byte) 3);
        pendingTask.setAssignee(testEmployee);
        pendingTask.setEndTime(LocalDateTime.now().plusDays(1));

        when(importantMatterRepository.findTopMatters(any(Pageable.class))).thenReturn(Collections.emptyList());
        when(importantTaskRepository.findTopTasks(any(Pageable.class))).thenReturn(Collections.emptyList());
        when(taskRepository.findTop10PersonalTasks(eq(userId), any(Pageable.class)))
                .thenReturn(Arrays.asList(pendingTask));
        when(logRepository.findTodayLogsByEmployeeId(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        HomepageDashboardDTO result = dashboardService.getDashboardData(userId);

        // Then
        assertThat(result.getPersonalTasks()).hasSize(1);
        assertThat(result.getPersonalTasks().get(0).getTaskStatus()).isEqualTo("未完成");
        assertThat(result.getPersonalTasks().get(0).getTaskPriority()).isEqualTo("低");
    }

    @Test
    @DisplayName("获取仪表板数据 - 验证重要任务状态转换")
    void testGetDashboardData_ImportantTaskStatusConversion() {
        // Given
        Integer userId = 1;
        ImportantTask completedTask = new ImportantTask();
        completedTask.setTaskId(1);
        completedTask.setTaskContent("已完成任务");
        completedTask.setTaskStatus((byte) 2); // 已完成
        completedTask.setTaskPriority((byte) 0); // 低优先级
        completedTask.setEmployee(testEmployee);
        completedTask.setDeadline(LocalDateTime.now());
        completedTask.setSerialNum((byte) 1);

        when(importantMatterRepository.findTopMatters(any(Pageable.class))).thenReturn(Collections.emptyList());
        when(importantTaskRepository.findTopTasks(any(Pageable.class)))
                .thenReturn(Arrays.asList(completedTask));
        when(taskRepository.findTop10PersonalTasks(eq(userId), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(logRepository.findTodayLogsByEmployeeId(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        HomepageDashboardDTO result = dashboardService.getDashboardData(userId);

        // Then
        assertThat(result.getCompanyTasks()).hasSize(1);
        assertThat(result.getCompanyTasks().get(0).getTaskStatus()).isEqualTo("已完成");
        assertThat(result.getCompanyTasks().get(0).getTaskPriority()).isEqualTo("低");
    }

    @Test
    @DisplayName("获取仪表板数据 - 验证无部门的重要事项")
    void testGetDashboardData_MatterWithoutDepartment() {
        // Given
        Integer userId = 1;
        ImportantMatter matterWithoutDept = new ImportantMatter();
        matterWithoutDept.setMatterId(1);
        matterWithoutDept.setTitle("全公司通知");
        matterWithoutDept.setContent("这是全公司通知");
        matterWithoutDept.setDepartment(null);
        matterWithoutDept.setPublishTime(LocalDateTime.now());

        when(importantMatterRepository.findTopMatters(any(Pageable.class)))
                .thenReturn(Arrays.asList(matterWithoutDept));
        when(importantTaskRepository.findTopTasks(any(Pageable.class))).thenReturn(Collections.emptyList());
        when(taskRepository.findTop10PersonalTasks(eq(userId), any(Pageable.class)))
                .thenReturn(Collections.emptyList());
        when(logRepository.findTodayLogsByEmployeeId(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // When
        HomepageDashboardDTO result = dashboardService.getDashboardData(userId);

        // Then
        assertThat(result.getCompanyNotices()).hasSize(1);
        assertThat(result.getCompanyNotices().get(0).getDepartmentName()).isNull();
    }
}
