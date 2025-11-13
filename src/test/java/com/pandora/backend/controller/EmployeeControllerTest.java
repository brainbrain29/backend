package com.pandora.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandora.backend.dto.EmployeeDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.enums.Gender;
import com.pandora.backend.filter.JwtAuthFilter;
import com.pandora.backend.service.EmployeeService;
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

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * EmployeeController 单元测试
 */
@WebMvcTest(value = EmployeeController.class,
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthFilter.class))
@DisplayName("Employee Controller 测试")
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    private Employee testEmployee;
    private EmployeeDTO testEmployeeDTO;

    @BeforeEach
    void setUp() {
        // 准备测试员工
        testEmployee = new Employee();
        testEmployee.setEmployeeId(1);
        testEmployee.setEmployeeName("张三");
        testEmployee.setGender(Gender.MALE);
        testEmployee.setPosition((byte) 2);
        testEmployee.setPhone("13800138000");
        testEmployee.setEmail("zhangsan@example.com");

        // 准备测试 DTO
        testEmployeeDTO = new EmployeeDTO();
        testEmployeeDTO.setEmployeeId(1);
        testEmployeeDTO.setEmployeeName("张三");
        testEmployeeDTO.setGender("男性");
        testEmployeeDTO.setPosition((byte) 2);
        testEmployeeDTO.setPhone("13800138000");
        testEmployeeDTO.setEmail("zhangsan@example.com");
    }

    @Test
    @DisplayName("创建员工 - 成功")
    void testCreateEmployee_Success() throws Exception {
        // Given
        when(employeeService.createEmployee(any(EmployeeDTO.class))).thenReturn(testEmployeeDTO);

        // When & Then
        mockMvc.perform(post("/employees")
                .with(user("admin").roles("ADMIN"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testEmployeeDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.employeeName").value("张三"))
                .andExpect(jsonPath("$.phone").value("13800138000"));
    }

    @Test
    @DisplayName("获取当前员工信息 - 成功")
    void testGetCurrentEmployee_Success() throws Exception {
        // Given
        when(employeeService.getEmployeeById(1)).thenReturn(testEmployee);

        // When & Then
        mockMvc.perform(get("/employees/me")
                .with(user("testuser").roles("USER"))
                .requestAttr("userId", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeName").value("张三"))
                .andExpect(jsonPath("$.gender").value("男性"))
                .andExpect(jsonPath("$.position").value(2));
    }

    @Test
    @DisplayName("获取可分配员工列表 - 成功（领导权限）")
    void testGetAssignableEmployees_Success() throws Exception {
        // Given - 当前用户是领导（position = 2）
        Employee leader = new Employee();
        leader.setEmployeeId(1);
        leader.setEmployeeName("领导");
        leader.setPosition((byte) 2);

        Employee employee1 = new Employee();
        employee1.setEmployeeId(2);
        employee1.setEmployeeName("员工1");
        employee1.setPosition((byte) 3);
        employee1.setPhone("13800138001");
        employee1.setEmail("emp1@example.com");

        Employee employee2 = new Employee();
        employee2.setEmployeeId(3);
        employee2.setEmployeeName("员工2");
        employee2.setPosition((byte) 3);
        employee2.setPhone("13800138002");
        employee2.setEmail("emp2@example.com");

        List<Employee> allEmployees = Arrays.asList(leader, employee1, employee2);

        when(employeeService.getEmployeeById(1)).thenReturn(leader);
        when(employeeService.getAllEmployees()).thenReturn(allEmployees);

        // When & Then
        mockMvc.perform(get("/employees/assignable")
                .with(user("leader").roles("LEADER"))
                .requestAttr("userId", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employees").isArray())
                .andExpect(jsonPath("$.employees.length()").value(2))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.message").value("获取成功"));
    }

    @Test
    @DisplayName("获取可分配员工列表 - 权限不足")
    void testGetAssignableEmployees_Forbidden() throws Exception {
        // Given - 当前用户是普通员工（position = 1，小于2）
        Employee normalEmployee = new Employee();
        normalEmployee.setEmployeeId(1);
        normalEmployee.setEmployeeName("普通员工");
        normalEmployee.setPosition((byte) 1);

        when(employeeService.getEmployeeById(1)).thenReturn(normalEmployee);

        // When & Then
        mockMvc.perform(get("/employees/assignable")
                .with(user("normaluser").roles("USER"))
                .requestAttr("userId", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().string("权限不足，只有领导才能分配任务"));
    }

    @Test
    @DisplayName("获取可分配员工列表 - 缺少 userId")
    void testGetAssignableEmployees_MissingUserId() throws Exception {
        // When & Then
        mockMvc.perform(get("/employees/assignable")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("获取可分配员工列表 - 用户不存在")
    void testGetAssignableEmployees_UserNotFound() throws Exception {
        // Given
        when(employeeService.getEmployeeById(999)).thenReturn(null);

        // When & Then
        mockMvc.perform(get("/employees/assignable")
                .with(user("testuser").roles("USER"))
                .requestAttr("userId", 999)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("获取可分配员工列表 - 验证排除自己")
    void testGetAssignableEmployees_ExcludeSelf() throws Exception {
        // Given
        Employee leader = new Employee();
        leader.setEmployeeId(1);
        leader.setEmployeeName("领导");
        leader.setPosition((byte) 2);
        leader.setPhone("13800138000");
        leader.setEmail("leader@example.com");

        List<Employee> allEmployees = Arrays.asList(leader);

        when(employeeService.getEmployeeById(1)).thenReturn(leader);
        when(employeeService.getAllEmployees()).thenReturn(allEmployees);

        // When & Then
        mockMvc.perform(get("/employees/assignable")
                .with(user("leader").roles("LEADER"))
                .requestAttr("userId", 1)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employees").isArray())
                .andExpect(jsonPath("$.employees.length()").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }
}
