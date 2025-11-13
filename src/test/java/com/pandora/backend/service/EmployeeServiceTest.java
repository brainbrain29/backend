package com.pandora.backend.service;

import com.pandora.backend.dto.EmployeeDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.enums.Gender;
import com.pandora.backend.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * EmployeeService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Employee Service 测试")
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee testEmployee;
    private EmployeeDTO testEmployeeDTO;

    @BeforeEach
    void setUp() {
        // 准备测试员工实体
        testEmployee = new Employee();
        testEmployee.setEmployeeId(1);
        testEmployee.setEmployeeName("张三");
        testEmployee.setGender(Gender.MALE);
        testEmployee.setPhone("13800138000");
        testEmployee.setEmail("zhangsan@example.com");
        testEmployee.setPosition((byte) 2);

        // 准备测试员工 DTO
        testEmployeeDTO = new EmployeeDTO();
        testEmployeeDTO.setEmployeeName("李四");
        testEmployeeDTO.setGender("女性");
        testEmployeeDTO.setPhone("13900139000");
        testEmployeeDTO.setEmail("lisi@example.com");
        testEmployeeDTO.setPosition((byte) 3);
    }

    @Test
    @DisplayName("创建员工 - 成功")
    void testCreateEmployee_Success() {
        // Given
        Employee savedEmployee = new Employee();
        savedEmployee.setEmployeeId(2);
        savedEmployee.setEmployeeName("李四");
        savedEmployee.setGender(Gender.FEMALE);
        savedEmployee.setPhone("13900139000");
        savedEmployee.setEmail("lisi@example.com");
        savedEmployee.setPosition((byte) 3);

        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);

        // When
        EmployeeDTO result = employeeService.createEmployee(testEmployeeDTO);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmployeeName()).isEqualTo("李四");
        assertThat(result.getGender()).isEqualTo("女性");
        assertThat(result.getPhone()).isEqualTo("13900139000");
        assertThat(result.getEmail()).isEqualTo("lisi@example.com");
        assertThat(result.getPosition()).isEqualTo((byte) 3);

        // Verify
        verify(employeeRepository).save(any(Employee.class));
    }

    @Test
    @DisplayName("创建员工 - 验证性别转换")
    void testCreateEmployee_GenderConversion() {
        // Given
        EmployeeDTO maleDTO = new EmployeeDTO();
        maleDTO.setEmployeeName("王五");
        maleDTO.setGender("男性");
        maleDTO.setPhone("13700137000");
        maleDTO.setEmail("wangwu@example.com");
        maleDTO.setPosition((byte) 3);

        Employee savedEmployee = new Employee();
        savedEmployee.setEmployeeId(3);
        savedEmployee.setEmployeeName("王五");
        savedEmployee.setGender(Gender.MALE);
        savedEmployee.setPhone("13700137000");
        savedEmployee.setEmail("wangwu@example.com");
        savedEmployee.setPosition((byte) 3);

        when(employeeRepository.save(any(Employee.class))).thenReturn(savedEmployee);

        // When
        EmployeeDTO result = employeeService.createEmployee(maleDTO);

        // Then
        assertThat(result.getGender()).isEqualTo("男性");
    }

    @Test
    @DisplayName("根据 ID 获取员工 - 成功")
    void testGetEmployeeById_Success() {
        // Given
        when(employeeRepository.findById(1)).thenReturn(Optional.of(testEmployee));

        // When
        Employee result = employeeService.getEmployeeById(1);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getEmployeeId()).isEqualTo(1);
        assertThat(result.getEmployeeName()).isEqualTo("张三");
        assertThat(result.getGender()).isEqualTo(Gender.MALE);
        assertThat(result.getPhone()).isEqualTo("13800138000");
        assertThat(result.getEmail()).isEqualTo("zhangsan@example.com");
        assertThat(result.getPosition()).isEqualTo((byte) 2);

        // Verify
        verify(employeeRepository).findById(1);
    }

    @Test
    @DisplayName("根据 ID 获取员工 - 员工不存在")
    void testGetEmployeeById_NotFound() {
        // Given
        when(employeeRepository.findById(999)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeService.getEmployeeById(999))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Employee not found with id: 999");

        // Verify
        verify(employeeRepository).findById(999);
    }

    @Test
    @DisplayName("获取所有员工 - 成功")
    void testGetAllEmployees_Success() {
        // Given
        Employee employee2 = new Employee();
        employee2.setEmployeeId(2);
        employee2.setEmployeeName("李四");
        employee2.setGender(Gender.FEMALE);
        employee2.setPosition((byte) 3);

        List<Employee> employees = Arrays.asList(testEmployee, employee2);
        when(employeeRepository.findAll()).thenReturn(employees);

        // When
        List<Employee> result = employeeService.getAllEmployees();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmployeeName()).isEqualTo("张三");
        assertThat(result.get(1).getEmployeeName()).isEqualTo("李四");

        // Verify
        verify(employeeRepository).findAll();
    }

    @Test
    @DisplayName("获取所有员工 - 空列表")
    void testGetAllEmployees_EmptyList() {
        // Given
        when(employeeRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<Employee> result = employeeService.getAllEmployees();

        // Then
        assertThat(result).isEmpty();

        // Verify
        verify(employeeRepository).findAll();
    }

    @Test
    @DisplayName("创建员工 - 验证不同职位")
    void testCreateEmployee_DifferentPositions() {
        // Given - 部门经理
        EmployeeDTO managerDTO = new EmployeeDTO();
        managerDTO.setEmployeeName("经理");
        managerDTO.setGender("男性");
        managerDTO.setPhone("13600136000");
        managerDTO.setEmail("manager@example.com");
        managerDTO.setPosition((byte) 1);

        Employee savedManager = new Employee();
        savedManager.setEmployeeId(4);
        savedManager.setEmployeeName("经理");
        savedManager.setGender(Gender.MALE);
        savedManager.setPhone("13600136000");
        savedManager.setEmail("manager@example.com");
        savedManager.setPosition((byte) 1);

        when(employeeRepository.save(any(Employee.class))).thenReturn(savedManager);

        // When
        EmployeeDTO result = employeeService.createEmployee(managerDTO);

        // Then
        assertThat(result.getPosition()).isEqualTo((byte) 1);
    }

    @Test
    @DisplayName("创建员工 - 验证团队长职位")
    void testCreateEmployee_TeamLeaderPosition() {
        // Given - 团队长
        EmployeeDTO leaderDTO = new EmployeeDTO();
        leaderDTO.setEmployeeName("团队长");
        leaderDTO.setGender("女性");
        leaderDTO.setPhone("13500135000");
        leaderDTO.setEmail("leader@example.com");
        leaderDTO.setPosition((byte) 2);

        Employee savedLeader = new Employee();
        savedLeader.setEmployeeId(5);
        savedLeader.setEmployeeName("团队长");
        savedLeader.setGender(Gender.FEMALE);
        savedLeader.setPhone("13500135000");
        savedLeader.setEmail("leader@example.com");
        savedLeader.setPosition((byte) 2);

        when(employeeRepository.save(any(Employee.class))).thenReturn(savedLeader);

        // When
        EmployeeDTO result = employeeService.createEmployee(leaderDTO);

        // Then
        assertThat(result.getPosition()).isEqualTo((byte) 2);
    }
}
