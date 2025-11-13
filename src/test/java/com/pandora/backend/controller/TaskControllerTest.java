package com.pandora.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pandora.backend.dto.TaskDTO;
import com.pandora.backend.dto.TaskStatusDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.service.LogService;
import com.pandora.backend.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TaskController 集成测试
 * 使用 MockMvc 测试 HTTP 端点，Service 层使用 Mock
 */
@WebMvcTest(controllers = TaskController.class, excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaskService taskService;

    @MockBean
    private LogService logService;

    @MockBean
    private EmployeeRepository employeeRepository;

    @MockBean
    private com.pandora.backend.util.JwtUtil jwtUtil;

    @MockBean
    private com.pandora.backend.filter.JwtAuthFilter jwtAuthFilter;

    private ObjectMapper objectMapper;
    private TaskDTO taskDTO;
    private Employee employee;

    @BeforeEach
    void setUp() {
        // 初始化 ObjectMapper，支持 Java 8 时间类型
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // 初始化测试数据
        employee = new Employee();
        employee.setEmployeeId(1);
        employee.setEmployeeName("张三");
        employee.setPosition((byte) 2);

        taskDTO = new TaskDTO();
        taskDTO.setTaskId(100);
        taskDTO.setTitle("完成项目报告");
        taskDTO.setContent("详细内容");
        taskDTO.setStartTime(LocalDateTime.now());
        taskDTO.setEndTime(LocalDateTime.now().plusDays(7));
        taskDTO.setTaskStatus("未开始");
        taskDTO.setTaskPriority("高");
        taskDTO.setTaskType("个人任务");
        taskDTO.setSenderId(1);
        taskDTO.setSenderName("张三");
        taskDTO.setAssigneeId(2);
        taskDTO.setAssigneeName("李四");
    }

    /**
     * 测试：成功创建任务
     */
    @Test
    void testCreateTask_Success() throws Exception {
        // Prepare request data
        TaskDTO requestDTO = new TaskDTO();
        requestDTO.setTitle("完成项目报告");
        requestDTO.setContent("详细内容");
        requestDTO.setSenderId(1);

        // Mock Service 响应
        when(taskService.createTask(any(TaskDTO.class))).thenReturn(taskDTO);

        // 执行请求并验证结果
        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO))
                .requestAttr("userId", 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value(100))
                .andExpect(jsonPath("$.title").value("完成项目报告"))
                .andExpect(jsonPath("$.senderName").value("张三"));

        verify(taskService, times(1)).createTask(any(TaskDTO.class));
    }

    /**
     * Test: Create task without authentication
     */
    @Test
    void testCreateTask_Unauthorized() throws Exception {
        // Prepare request data
        TaskDTO requestDTO = new TaskDTO();
        requestDTO.setTitle("完成项目报告");
        requestDTO.setSenderId(1);

        // Execute without userId attribute
        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO)))
                .andExpect(status().isUnauthorized());

        verify(taskService, never()).createTask(any(TaskDTO.class));
    }

    /**
     * Test: Create task with taskId should fail
     */
    @Test
    void testCreateTask_WithTaskId() throws Exception {
        // Prepare request data with taskId
        TaskDTO requestDTO = new TaskDTO();
        requestDTO.setTaskId(100);
        requestDTO.setTitle("完成项目报告");
        requestDTO.setSenderId(1);

        // Execute and verify
        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO))
                .requestAttr("userId", 1))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createTask(any(TaskDTO.class));
    }

    /**
     * Test: Create task without sender ID
     */
    @Test
    void testCreateTask_MissingSenderId() throws Exception {
        // Prepare request data without senderId
        TaskDTO requestDTO = new TaskDTO();
        requestDTO.setTitle("完成项目报告");

        // Execute and verify
        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO))
                .requestAttr("userId", 1))
                .andExpect(status().isBadRequest());

        verify(taskService, never()).createTask(any(TaskDTO.class));
    }

    /**
     * Test: Create task with sender mismatch
     */
    @Test
    void testCreateTask_SenderMismatch() throws Exception {
        // Prepare request data with different senderId
        TaskDTO requestDTO = new TaskDTO();
        requestDTO.setTitle("完成项目报告");
        requestDTO.setSenderId(2);

        // Execute and verify
        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO))
                .requestAttr("userId", 1))
                .andExpect(status().isForbidden());

        verify(taskService, never()).createTask(any(TaskDTO.class));
    }

    /**
     * Test: Assign task successfully (as team leader)
     */
    @Test
    void testAssignTask_Success() throws Exception {
        // Prepare request data
        TaskDTO requestDTO = new TaskDTO();
        requestDTO.setTitle("团队任务");
        requestDTO.setSenderId(1);
        requestDTO.setAssigneeId(2);

        // Mock repository and service
        when(employeeRepository.findById(1)).thenReturn(Optional.of(employee));
        when(taskService.createTask(any(TaskDTO.class))).thenReturn(taskDTO);

        // Execute and verify
        mockMvc.perform(post("/tasks/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO))
                .requestAttr("userId", 1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value(100));

        verify(taskService, times(1)).createTask(any(TaskDTO.class));
    }

    /**
     * 测试：没有权限分配任务（职位 < 2）
     */
    @Test
    void testAssignTask_NoPermission() throws Exception {
        // 准备没有权限的员工（普通员工，职位 = 1）
        Employee regularEmployee = new Employee();
        regularEmployee.setEmployeeId(1);
        regularEmployee.setPosition((byte) 1);  // 职位 < 2，没有分配权限

        // 准备请求数据
        TaskDTO requestDTO = new TaskDTO();
        requestDTO.setTitle("团队任务");
        requestDTO.setSenderId(1);

        // Mock repository
        when(employeeRepository.findById(1)).thenReturn(Optional.of(regularEmployee));

        // 执行请求并验证结果
        mockMvc.perform(post("/tasks/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDTO))
                .requestAttr("userId", 1))
                .andExpect(status().isForbidden());

        verify(taskService, never()).createTask(any(TaskDTO.class));
    }

    /**
     * Test: Update task successfully
     */
    @Test
    void testUpdateTask_Success() throws Exception {
        // Prepare update data
        TaskDTO updateDTO = new TaskDTO();
        updateDTO.setTitle("更新后的标题");
        updateDTO.setSenderId(1);

        // Mock service response
        when(taskService.updateTask(eq(100), any(TaskDTO.class))).thenReturn(taskDTO);

        // Execute and verify
        mockMvc.perform(put("/tasks/100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO))
                .requestAttr("userId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(100));

        verify(taskService, times(1)).updateTask(eq(100), any(TaskDTO.class));
    }

    /**
     * Test: Update task with sender mismatch
     */
    @Test
    void testUpdateTask_SenderMismatch() throws Exception {
        // Prepare update data with different senderId
        TaskDTO updateDTO = new TaskDTO();
        updateDTO.setTitle("更新后的标题");
        updateDTO.setSenderId(2);

        // Execute and verify
        mockMvc.perform(put("/tasks/100")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO))
                .requestAttr("userId", 1))
                .andExpect(status().isForbidden());

        verify(taskService, never()).updateTask(any(), any());
    }

    /**
     * Test: Update non-existent task
     */
    @Test
    void testUpdateTask_NotFound() throws Exception {
        // Prepare update data
        TaskDTO updateDTO = new TaskDTO();
        updateDTO.setTitle("更新后的标题");
        updateDTO.setSenderId(1);

        // Mock service to throw exception
        when(taskService.updateTask(eq(999), any(TaskDTO.class)))
                .thenThrow(new RuntimeException("Task not found"));

        // Execute and verify
        mockMvc.perform(put("/tasks/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO))
                .requestAttr("userId", 1))
                .andExpect(status().isNotFound());
    }

    /**
     * Test: Delete task successfully
     */
    @Test
    void testDeleteTask_Success() throws Exception {
        // Mock service
        doNothing().when(taskService).deleteTask(100);

        // Execute and verify
        mockMvc.perform(delete("/tasks/100")
                .requestAttr("userId", 1))
                .andExpect(status().isNoContent());

        verify(taskService, times(1)).deleteTask(100);
    }

    /**
     * Test: Delete non-existent task
     */
    @Test
    void testDeleteTask_NotFound() throws Exception {
        // Mock service to throw exception
        doThrow(new RuntimeException("Task not found")).when(taskService).deleteTask(999);

        // Execute and verify
        mockMvc.perform(delete("/tasks/999")
                .requestAttr("userId", 1))
                .andExpect(status().isNotFound());
    }

    /**
     * Test: Get task by ID successfully
     */
    @Test
    void testGetTaskById_Success() throws Exception {
        // Mock service response
        when(taskService.getTaskById(100)).thenReturn(taskDTO);

        // Execute and verify
        mockMvc.perform(get("/tasks/100")
                .requestAttr("userId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(100))
                .andExpect(jsonPath("$.title").value("完成项目报告"));

        verify(taskService, times(1)).getTaskById(100);
    }

    /**
     * 测试：获取所有任务
     */
    @Test
    void testGetAllTasks() throws Exception {
        // 准备测试数据
        List<TaskDTO> tasks = Arrays.asList(taskDTO);

        // Mock service 响应（注意：Controller 实际调用的是 getTasksByUserId）
        when(taskService.getTasksByUserId(1)).thenReturn(tasks);

        // 执行请求并验证结果
        mockMvc.perform(get("/tasks")
                .requestAttr("userId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value(100))
                .andExpect(jsonPath("$[0].title").value("完成项目报告"));

        verify(taskService, times(1)).getTasksByUserId(1);
    }

    /**
     * Test: Search tasks with keyword
     */
    @Test
    void testSearchTasks() throws Exception {
        // Prepare test data
        List<TaskDTO> tasks = Arrays.asList(taskDTO);

        // Mock service response
        when(taskService.searchTasks("项目")).thenReturn(tasks);

        // Execute and verify
        mockMvc.perform(get("/tasks/search")
                .param("keyword", "项目")
                .requestAttr("userId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("完成项目报告"));

        verify(taskService, times(1)).searchTasks("项目");
    }

    /**
     * 测试：根据发送者ID获取任务
     */
    @Test
    void testGetTasksByUserId() throws Exception {
        // 准备测试数据
        List<TaskDTO> tasks = Arrays.asList(taskDTO);

        // Mock service 响应
        when(taskService.getTasksBySenderId(1)).thenReturn(tasks);

        // 执行请求并验证结果（使用正确的端点 /tasks/sender/{senderId}）
        mockMvc.perform(get("/tasks/sender/1")
                .requestAttr("userId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value(100));

        verify(taskService, times(1)).getTasksBySenderId(1);
    }

    /**
     * 测试：成功更新任务状态
     */
    @Test
    void testUpdateTaskStatus_Success() throws Exception {
        // 准备请求数据
        TaskStatusDTO statusDTO = new TaskStatusDTO();
        statusDTO.setTaskId(100);
        statusDTO.setTaskStatus("进行中");

        // Mock repository 和 service
        when(employeeRepository.findById(1)).thenReturn(Optional.of(employee));
        when(taskService.updateTaskStatus(any(TaskStatusDTO.class), eq(1), eq((byte) 2)))
                .thenReturn(taskDTO);

        // 执行请求并验证结果（使用正确的路径 /tasks/{taskId}/status）
        mockMvc.perform(put("/tasks/100/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(statusDTO))
                .requestAttr("userId", 1)
                .requestAttr("position", (byte) 2))  // 添加 position 属性
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(100));

        verify(taskService, times(1)).updateTaskStatus(any(TaskStatusDTO.class), eq(1), eq((byte) 2));
    }

    /**
     * 测试：根据里程碑ID获取任务
     */
    @Test
    void testGetTasksByMilestoneId() throws Exception {
        // 准备测试数据（添加 milestoneName）
        taskDTO.setMilestoneName("里程碑1");
        List<TaskDTO> tasks = Arrays.asList(taskDTO);

        // Mock service 响应
        when(taskService.getTasksByMilestoneId(10)).thenReturn(tasks);

        // 执行请求并验证结果
        mockMvc.perform(get("/tasks/milestone/10")
                .requestAttr("userId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].milestoneName").value("里程碑1"));

        verify(taskService, times(1)).getTasksByMilestoneId(10);
    }

    /**
     * Test: Get tasks by team (as team leader)
     */
    @Test
    void testGetTasksByTeam() throws Exception {
        // Prepare test data
        List<TaskDTO> tasks = Arrays.asList(taskDTO);

        // Mock repository and service
        when(employeeRepository.findById(1)).thenReturn(Optional.of(employee));
        when(taskService.getTasksByTeam(1)).thenReturn(tasks);

        // Execute and verify
        mockMvc.perform(get("/tasks/team")
                .requestAttr("userId", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taskId").value(100));

        verify(taskService, times(1)).getTasksByTeam(1);
    }
}
