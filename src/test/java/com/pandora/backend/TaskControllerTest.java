package com.pandora.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandora.backend.dto.TaskDTO;
import com.pandora.backend.service.TaskService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional  // 每个测试后回滚数据库
public class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskService taskService;

    /**
     * 测试创建任务
     */
    @Test
    public void testCreateTask() throws Exception {
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setTitle("测试任务");
        taskDTO.setContent("这是一个测试任务");
        taskDTO.setStartTime(LocalDateTime.now());
        taskDTO.setEndTime(LocalDateTime.now().plusDays(5));
        taskDTO.setTaskStatus((byte) 0);
        taskDTO.setTaskPriority((byte) 1);
        taskDTO.setSenderId(1);
        taskDTO.setAssigneeId(2);
        taskDTO.setTaskType((byte) 1);
        taskDTO.setCreatedByWho((byte) 1);
        taskDTO.setMilestoneId(1);

        mockMvc.perform(post("/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(taskDTO)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("测试任务"));
    }

    /**
     * 测试查询所有任务
     */
    @Test
    public void testGetAllTasks() throws Exception {
        mockMvc.perform(get("/tasks"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * 测试查询单个任务
     */
    @Test
    public void testGetTaskById() throws Exception {
        // 先创建一个任务
        TaskDTO taskDTO = createTestTask();
        TaskDTO created = taskService.createTask(taskDTO);

        // 查询该任务
        mockMvc.perform(get("/tasks/" + created.getTaskId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.taskId").value(created.getTaskId()))
                .andExpect(jsonPath("$.title").value("测试任务"));
    }

    /**
     * 测试更新任务
     */
    @Test
    public void testUpdateTask() throws Exception {
        // 先创建一个任务
        TaskDTO taskDTO = createTestTask();
        TaskDTO created = taskService.createTask(taskDTO);

        // 更新任务
        TaskDTO updateDTO = new TaskDTO();
        updateDTO.setTitle("更新后的任务");
        updateDTO.setTaskStatus((byte) 1);

        mockMvc.perform(put("/tasks/" + created.getTaskId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDTO)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("更新后的任务"))
                .andExpect(jsonPath("$.taskStatus").value(1));
    }

    /**
     * 测试删除任务
     */
    @Test
    public void testDeleteTask() throws Exception {
        // 先创建一个任务
        TaskDTO taskDTO = createTestTask();
        TaskDTO created = taskService.createTask(taskDTO);

        // 删除任务
        mockMvc.perform(delete("/tasks/" + created.getTaskId()))
                .andDo(print())
                .andExpect(status().isNoContent());

        // 验证已删除
        mockMvc.perform(get("/tasks/" + created.getTaskId()))
                .andExpect(status().isNotFound());
    }

    /**
     * 测试按发送者查询
     */
    @Test
    public void testGetTasksBySenderId() throws Exception {
        mockMvc.perform(get("/tasks/sender/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * 测试按执行者查询
     */
    @Test
    public void testGetTasksByAssigneeId() throws Exception {
        mockMvc.perform(get("/tasks/assignee/2"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * 测试按里程碑查询
     */
    @Test
    public void testGetTasksByMilestoneId() throws Exception {
        mockMvc.perform(get("/tasks/milestone/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * 测试按状态查询
     */
    @Test
    public void testGetTasksByStatus() throws Exception {
        mockMvc.perform(get("/tasks/status/0"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * 辅助方法：创建测试任务
     */
    private TaskDTO createTestTask() {
        TaskDTO taskDTO = new TaskDTO();
        taskDTO.setTitle("测试任务");
        taskDTO.setContent("这是一个测试任务");
        taskDTO.setStartTime(LocalDateTime.now());
        taskDTO.setEndTime(LocalDateTime.now().plusDays(5));
        taskDTO.setTaskStatus((byte) 0);
        taskDTO.setTaskPriority((byte) 1);
        taskDTO.setSenderId(1);
        taskDTO.setAssigneeId(2);
        taskDTO.setTaskType((byte) 1);
        taskDTO.setCreatedByWho((byte) 1);
        taskDTO.setMilestoneId(1);
        return taskDTO;
    }
}


