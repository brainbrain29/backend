package com.pandora.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandora.backend.dto.TaskDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.Task;
import com.pandora.backend.enums.Gender;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.repository.TaskRepository;
import com.pandora.backend.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // 每个测试后回滚数据库
public class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskService taskService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private TaskRepository taskRepository;

    private Employee sender;
    private Employee assignee;

    @BeforeEach
    void setUp() {
        // 创建 sender 和 assignee
        sender = new Employee();
        sender.setEmployeeName("发送者");
        sender.setGender(Gender.UNKNOWN);
        sender.setEmail("sender@example.com");
        sender.setPhone("1234567890");
        sender.setPosition((byte) 1);
        sender.setPassword("123456");
        sender = employeeRepository.save(sender);

        assignee = new Employee();
        assignee.setEmployeeName("执行者");
        assignee.setGender(Gender.UNKNOWN);
        assignee.setEmail("assignee@example.com");
        assignee.setPhone("0987654321");
        assignee.setPosition((byte) 1);
        assignee.setPassword("123456");
        assignee = employeeRepository.save(assignee);
    }

    @Test
    void testCreateAndGetTask() throws Exception {
        TaskDTO dto = new TaskDTO();
        dto.setTitle("集成测试任务");
        dto.setContent("这是集成测试内容");
        dto.setStartTime(LocalDateTime.now());
        dto.setEndTime(LocalDateTime.now().plusDays(3));
        dto.setTaskStatus((byte) 0);
        dto.setTaskPriority((byte) 1);
        dto.setSenderId(sender.getEmployeeId());
        dto.setAssigneeId(assignee.getEmployeeId());
        dto.setTaskType((byte) 1);
        dto.setCreatedByWho((byte) 1);

        // POST 创建任务
        String response = mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("集成测试任务"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        TaskDTO created = objectMapper.readValue(response, TaskDTO.class);

        // GET 验证任务
        mockMvc.perform(get("/tasks/" + created.getTaskId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("集成测试任务"))
                .andExpect(jsonPath("$.senderId").value(sender.getEmployeeId()))
                .andExpect(jsonPath("$.assigneeId").value(assignee.getEmployeeId()));
    }

    @Test
    void testUpdateTaskStatus() throws Exception {
        // 使用 Service 创建任务
        Task task = new Task();
        task.setTitle("待更新任务");
        task.setContent("原始内容");
        task.setTaskStatus((byte) 0);
        task.setTaskPriority((byte) 1);
        task.setSender(sender);
        task.setAssignee(assignee);
        task.setTaskType((byte) 1);
        task.setCreatedByWho((byte) 1);
        task = taskRepository.save(task);

        TaskDTO updateDTO = new TaskDTO();
        updateDTO.setTaskStatus((byte) 2); // 更新为已完成

        mockMvc.perform(put("/tasks/" + task.getTaskId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskStatus").value(2));
    }

    @Test
    void testDeleteTask() throws Exception {
        // 使用 Service 创建任务
        Task task = new Task();
        task.setTitle("待删除任务");
        task.setContent("删除测试");
        task.setTaskStatus((byte) 0);
        task.setTaskPriority((byte) 1);
        task.setSender(sender);
        task.setAssignee(assignee);
        task.setTaskType((byte) 1);
        task.setCreatedByWho((byte) 1);
        task = taskRepository.save(task);

        mockMvc.perform(delete("/tasks/" + task.getTaskId()))
                .andDo(print())
                .andExpect(status().isNoContent());

        // 验证数据库中已删除
        assert (!taskRepository.existsById(task.getTaskId()));
    }
}
