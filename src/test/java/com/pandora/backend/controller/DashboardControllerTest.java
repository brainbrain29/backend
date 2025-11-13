package com.pandora.backend.controller;

import com.pandora.backend.dto.HomepageDashboardDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.enums.Gender;
import com.pandora.backend.filter.JwtAuthFilter;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * DashboardController 单元测试
 */
@WebMvcTest(value = DashboardController.class, 
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@DisplayName("Dashboard Controller 测试")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private EmployeeRepository employeeRepository;

    private Employee testEmployee;
    private HomepageDashboardDTO testDashboard;

    @BeforeEach
    void setUp() {
        // 准备测试员工
        testEmployee = new Employee();
        testEmployee.setEmployeeId(1);
        testEmployee.setEmployeeName("张三");
        testEmployee.setGender(Gender.MALE);
        testEmployee.setPosition((byte) 2);

        // 准备测试仪表板数据
        testDashboard = new HomepageDashboardDTO();
        testDashboard.setCompanyNotices(java.util.Collections.emptyList());
        testDashboard.setCompanyTasks(java.util.Collections.emptyList());
        testDashboard.setPersonalTasks(java.util.Collections.emptyList());
        testDashboard.setTodayLogs(java.util.Collections.emptyList());
    }

    @Test
    @DisplayName("获取仪表板数据 - 成功")
    void testGetHomepageDashboard_Success() throws Exception {
        // Given
        when(employeeRepository.findById(1)).thenReturn(Optional.of(testEmployee));
        when(dashboardService.getDashboardData(1)).thenReturn(testDashboard);

        // When & Then
        mockMvc.perform(get("/dashboard")
                .with(user("testuser").roles("USER"))
                .requestAttr("userId", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companyNotices").isArray())
                .andExpect(jsonPath("$.companyTasks").isArray())
                .andExpect(jsonPath("$.personalTasks").isArray())
                .andExpect(jsonPath("$.todayLogs").isArray());
    }

    @Test
    @DisplayName("获取仪表板数据 - 缺少 userId")
    void testGetHomepageDashboard_MissingUserId() throws Exception {
        // When & Then
        // 注意：由于排除了 JwtAuthFilter，这个测试实际上无法模拟缺少 userId 的情况
        // 因为 @RequestAttribute 会在没有 Filter 的情况下返回 null
        // 这里我们直接测试 Controller 的逻辑
        mockMvc.perform(get("/dashboard")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取仪表板数据 - 用户不存在")
    void testGetHomepageDashboard_UserNotFound() throws Exception {
        // Given
        when(employeeRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/dashboard")
                .with(user("testuser").roles("USER"))
                .requestAttr("userId", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取仪表板数据 - 验证 Service 调用")
    void testGetHomepageDashboard_VerifyServiceCall() throws Exception {
        // Given
        when(employeeRepository.findById(any())).thenReturn(Optional.of(testEmployee));
        when(dashboardService.getDashboardData(any())).thenReturn(testDashboard);

        // When
        mockMvc.perform(get("/dashboard")
                .with(user("testuser").roles("USER"))
                .requestAttr("userId", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Then - 验证 Service 方法被调用
        org.mockito.Mockito.verify(dashboardService).getDashboardData(1);
    }
}
