package com.pandora.backend.service;

import com.pandora.backend.dto.TaskDTO;
import com.pandora.backend.dto.TaskStatusDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.Milestone;
import com.pandora.backend.entity.Task;
import com.pandora.backend.enums.Priority;
import com.pandora.backend.enums.Status;
import com.pandora.backend.enums.TaskType;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.repository.EmployeeTeamRepository;
import com.pandora.backend.repository.MilestoneRepository;
import com.pandora.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * TaskService Unit Test
 * Tests business logic without real database dependency
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private NoticeService noticeService;

    @Mock
    private EmployeeTeamRepository employeeTeamRepository;

    @InjectMocks
    private TaskService taskService;

    private Employee sender;
    private Employee assignee;
    private Milestone milestone;
    private Task task;
    private TaskDTO taskDTO;

    @BeforeEach
    void setUp() {
        // Initialize test data
        sender = new Employee();
        sender.setEmployeeId(1);
        sender.setEmployeeName("张三");
        sender.setPosition((byte) 2);

        assignee = new Employee();
        assignee.setEmployeeId(2);
        assignee.setEmployeeName("李四");
        assignee.setPosition((byte) 3);

        milestone = new Milestone();
        milestone.setMilestoneId(10);
        milestone.setTitle("里程碑1");

        task = new Task();
        task.setTaskId(100);
        task.setTitle("完成项目报告");
        task.setContent("详细内容");
        task.setStartTime(LocalDateTime.now());
        task.setEndTime(LocalDateTime.now().plusDays(7));
        task.setTaskStatus((byte) Status.NOT_FINISHED.getCode());
        task.setTaskPriority((byte) Priority.HIGH.getCode());
        task.setTaskType((byte) TaskType.YES.getCode());
        task.setSender(sender);
        task.setAssignee(assignee);
        task.setMilestone(milestone);

        taskDTO = new TaskDTO();
        taskDTO.setTitle("完成项目报告");
        taskDTO.setContent("详细内容");
        taskDTO.setStartTime(LocalDateTime.now());
        taskDTO.setEndTime(LocalDateTime.now().plusDays(7));
        taskDTO.setTaskStatus("未开始");
        taskDTO.setTaskPriority("高");
        taskDTO.setTaskType("个人任务");
        taskDTO.setSenderId(1);
        taskDTO.setAssigneeId(2);
        taskDTO.setMilestoneId(10);
    }

    /**
     * Test: Create task successfully
     */
    @Test
    void testCreateTask_Success() {
        // Mock repository responses
        when(employeeRepository.findById(1)).thenReturn(Optional.of(sender));
        when(employeeRepository.findById(2)).thenReturn(Optional.of(assignee));
        when(milestoneRepository.findById(10)).thenReturn(Optional.of(milestone));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        doNothing().when(noticeService).createTaskAssignmentNotice(any(Task.class));

        // Execute
        TaskDTO result = taskService.createTask(taskDTO);

        // Verify
        assertNotNull(result);
        assertEquals("完成项目报告", result.getTitle());
        assertEquals("详细内容", result.getContent());
        assertEquals("张三", result.getSenderName());
        assertEquals("李四", result.getAssigneeName());
        assertEquals("里程碑1", result.getMilestoneName());

        // Verify repository calls
        verify(employeeRepository, times(1)).findById(1);
        verify(employeeRepository, times(1)).findById(2);
        verify(milestoneRepository, times(1)).findById(10);
        verify(taskRepository, times(1)).save(any(Task.class));
        verify(noticeService, times(1)).createTaskAssignmentNotice(any(Task.class));
    }

    /**
     * Test: Create task without sender ID should throw exception
     */
    @Test
    void testCreateTask_MissingSenderId() {
        // Prepare data without sender ID
        taskDTO.setSenderId(null);

        // Execute and verify exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskService.createTask(taskDTO);
        });

        assertEquals("Sender ID is required", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    /**
     * Test: Create task with non-existent sender should throw exception
     */
    @Test
    void testCreateTask_SenderNotFound() {
        // Mock sender not found
        when(employeeRepository.findById(1)).thenReturn(Optional.empty());

        // Execute and verify exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskService.createTask(taskDTO);
        });

        assertEquals("Sender not found", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    /**
     * Test: Create task with non-existent assignee should throw exception
     */
    @Test
    void testCreateTask_AssigneeNotFound() {
        // Mock sender exists but assignee not found
        when(employeeRepository.findById(1)).thenReturn(Optional.of(sender));
        when(employeeRepository.findById(2)).thenReturn(Optional.empty());

        // Execute and verify exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskService.createTask(taskDTO);
        });

        assertEquals("Assignee not found", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    /**
     * Test: Create task without assignee (company task)
     */
    @Test
    void testCreateTask_WithoutAssignee() {
        // Prepare data without assignee
        taskDTO.setAssigneeId(null);
        task.setAssignee(null);

        // Mock repository responses
        when(employeeRepository.findById(1)).thenReturn(Optional.of(sender));
        when(milestoneRepository.findById(10)).thenReturn(Optional.of(milestone));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        lenient().doNothing().when(noticeService).createTaskAssignmentNotice(any(Task.class));

        // Execute
        TaskDTO result = taskService.createTask(taskDTO);

        // Verify
        assertNotNull(result);
        assertNull(result.getAssigneeId());
        assertNull(result.getAssigneeName());
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    /**
     * Test: Update task successfully
     */
    @Test
    void testUpdateTask_Success() {
        // Mock repository responses
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Prepare update data
        TaskDTO updateDTO = new TaskDTO();
        updateDTO.setTitle("更新后的标题");
        updateDTO.setTaskStatus("进行中");

        // Execute
        TaskDTO result = taskService.updateTask(100, updateDTO);

        // Verify
        assertNotNull(result);
        verify(taskRepository, times(1)).findById(100);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    /**
     * Test: Update non-existent task should throw exception
     */
    @Test
    void testUpdateTask_TaskNotFound() {
        // Mock task not found
        when(taskRepository.findById(999)).thenReturn(Optional.empty());

        // Execute and verify exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskService.updateTask(999, taskDTO);
        });

        assertEquals("Task not found", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    /**
     * Test: Delete task successfully
     */
    @Test
    void testDeleteTask_Success() {
        // Mock task exists
        when(taskRepository.existsById(100)).thenReturn(true);
        doNothing().when(taskRepository).deleteById(100);

        // Execute
        taskService.deleteTask(100);

        // Verify
        verify(taskRepository, times(1)).existsById(100);
        verify(taskRepository, times(1)).deleteById(100);
    }

    /**
     * Test: Delete non-existent task should throw exception
     */
    @Test
    void testDeleteTask_TaskNotFound() {
        // Mock task not found
        when(taskRepository.existsById(999)).thenReturn(false);

        // Execute and verify exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskService.deleteTask(999);
        });

        assertEquals("Task not found", exception.getMessage());
        verify(taskRepository, never()).deleteById(any());
    }

    /**
     * Test: Get task by ID successfully
     */
    @Test
    void testGetTaskById_Success() {
        // Mock repository response
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));

        // Execute
        TaskDTO result = taskService.getTaskById(100);

        // Verify
        assertNotNull(result);
        assertEquals(100, result.getTaskId());
        assertEquals("完成项目报告", result.getTitle());
        verify(taskRepository, times(1)).findById(100);
    }

    /**
     * Test: Get non-existent task should throw exception
     */
    @Test
    void testGetTaskById_TaskNotFound() {
        // Mock task not found
        when(taskRepository.findById(999)).thenReturn(Optional.empty());

        // Execute and verify exception
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskService.getTaskById(999);
        });

        assertEquals("Task not found", exception.getMessage());
    }

    /**
     * Test: Get all tasks
     */
    @Test
    void testGetAllTasks() {
        // Prepare test data
        List<Task> tasks = Arrays.asList(task);

        // Mock repository response
        when(taskRepository.findAll()).thenReturn(tasks);

        // Execute
        List<TaskDTO> result = taskService.getAllTasks();

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("完成项目报告", result.get(0).getTitle());
        verify(taskRepository, times(1)).findAll();
    }

    /**
     * Test: Search tasks with keyword
     */
    @Test
    void testSearchTasks_WithKeyword() {
        // Prepare test data
        List<Task> tasks = Arrays.asList(task);

        // Mock repository response
        when(taskRepository.searchByKeyword("项目")).thenReturn(tasks);

        // Execute
        List<TaskDTO> result = taskService.searchTasks("项目");

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskRepository, times(1)).searchByKeyword("项目");
    }

    /**
     * Test: Search tasks with null keyword
     */
    @Test
    void testSearchTasks_NullKeyword() {
        // Execute
        List<TaskDTO> result = taskService.searchTasks(null);

        // Verify
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(taskRepository, never()).searchByKeyword(any());
    }

    /**
     * Test: Search tasks with empty keyword
     */
    @Test
    void testSearchTasks_EmptyKeyword() {
        // Execute
        List<TaskDTO> result = taskService.searchTasks("   ");

        // Verify
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(taskRepository, never()).searchByKeyword(any());
    }

    /**
     * Test: Get tasks by user ID (created and assigned)
     */
    @Test
    void testGetTasksByUserId() {
        // Prepare test data
        Task task2 = new Task();
        task2.setTaskId(101);
        task2.setTitle("另一个任务");
        task2.setSender(sender);
        task2.setAssignee(assignee);
        task2.setTaskStatus((byte) 0);
        task2.setTaskPriority((byte) 1);
        task2.setTaskType((byte) 0);

        // Mock repository responses
        when(taskRepository.findBySenderEmployeeId(1)).thenReturn(Arrays.asList(task));
        when(taskRepository.findByAssigneeEmployeeId(1)).thenReturn(Arrays.asList(task2));

        // Execute
        List<TaskDTO> result = taskService.getTasksByUserId(1);

        // Verify
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(taskRepository, times(1)).findBySenderEmployeeId(1);
        verify(taskRepository, times(1)).findByAssigneeEmployeeId(1);
    }

    /**
     * Test: Get tasks by sender ID
     */
    @Test
    void testGetTasksBySenderId() {
        // Mock repository response
        when(taskRepository.findBySenderEmployeeId(1)).thenReturn(Arrays.asList(task));

        // Execute
        List<TaskDTO> result = taskService.getTasksBySenderId(1);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskRepository, times(1)).findBySenderEmployeeId(1);
    }

    /**
     * Test: Get tasks by assignee ID
     */
    @Test
    void testGetTasksByAssigneeId() {
        // Mock repository response
        when(taskRepository.findByAssigneeEmployeeId(2)).thenReturn(Arrays.asList(task));

        // Execute
        List<TaskDTO> result = taskService.getTasksByAssigneeId(2);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskRepository, times(1)).findByAssigneeEmployeeId(2);
    }

    /**
     * Test: Get tasks by milestone ID
     */
    @Test
    void testGetTasksByMilestoneId() {
        // Mock repository response
        when(taskRepository.findByMilestoneMilestoneId(10)).thenReturn(Arrays.asList(task));

        // Execute
        List<TaskDTO> result = taskService.getTasksByMilestoneId(10);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskRepository, times(1)).findByMilestoneMilestoneId(10);
    }

    /**
     * Test: Get tasks by status
     */
    @Test
    void testGetTasksByStatus() {
        // Mock repository response
        when(taskRepository.findByTaskStatus((byte) 0)).thenReturn(Arrays.asList(task));

        // Execute
        List<TaskDTO> result = taskService.getTasksByStatus((byte) 0);

        // Verify
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(taskRepository, times(1)).findByTaskStatus((byte) 0);
    }

    /**
     * Test: Update task status successfully (as assignee)
     */
    @Test
    void testUpdateTaskStatus_AsAssignee() {
        // Prepare data
        TaskStatusDTO statusDTO = new TaskStatusDTO();
        statusDTO.setTaskId(100);
        statusDTO.setTaskStatus("进行中");

        // Mock repository responses
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Execute (user is assignee)
        TaskDTO result = taskService.updateTaskStatus(statusDTO, 2, (byte) 3);

        // Verify
        assertNotNull(result);
        verify(taskRepository, times(1)).findById(100);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    /**
     * Test: Update task status successfully (as sender)
     */
    @Test
    void testUpdateTaskStatus_AsSender() {
        // Prepare data
        TaskStatusDTO statusDTO = new TaskStatusDTO();
        statusDTO.setTaskId(100);
        statusDTO.setTaskStatus("已完成");

        // Mock repository responses
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);

        // Execute (user is sender)
        TaskDTO result = taskService.updateTaskStatus(statusDTO, 1, (byte) 2);

        // Verify
        assertNotNull(result);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    /**
     * Test: Update task status without permission
     */
    @Test
    void testUpdateTaskStatus_NoPermission() {
        // Prepare data
        TaskStatusDTO statusDTO = new TaskStatusDTO();
        statusDTO.setTaskId(100);
        statusDTO.setTaskStatus("进行中");

        // Mock repository response
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));

        // Execute and verify exception (user is neither assignee nor sender)
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.updateTaskStatus(statusDTO, 999, (byte) 3);
        });

        assertEquals("无权限更新此任务状态", exception.getMessage());
        verify(taskRepository, never()).save(any(Task.class));
    }

    /**
     * Test: Update task status with null task ID
     */
    @Test
    void testUpdateTaskStatus_NullTaskId() {
        // Prepare data
        TaskStatusDTO statusDTO = new TaskStatusDTO();
        statusDTO.setTaskId(null);
        statusDTO.setTaskStatus("进行中");

        // Execute and verify exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.updateTaskStatus(statusDTO, 1, (byte) 2);
        });

        assertEquals("任务ID不能为空", exception.getMessage());
    }

    /**
     * Test: Update task status with null status
     */
    @Test
    void testUpdateTaskStatus_NullStatus() {
        // Prepare data
        TaskStatusDTO statusDTO = new TaskStatusDTO();
        statusDTO.setTaskId(100);
        statusDTO.setTaskStatus(null);

        // Execute and verify exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            taskService.updateTaskStatus(statusDTO, 1, (byte) 2);
        });

        assertEquals("任务状态不能为空", exception.getMessage());
    }

    /**
     * Test: Update task with all null fields (should keep original values)
     */
    @Test
    void testUpdateTask_AllNullFields() {
        TaskDTO updateDTO = new TaskDTO();
        // 所有字段都为 null
        
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        
        TaskDTO result = taskService.updateTask(100, updateDTO);
        
        assertNotNull(result);
        // 原始值应该保持不变
        assertEquals("完成项目报告", result.getTitle());
    }

    /**
     * Test: Update task - only update title
     */
    @Test
    void testUpdateTask_OnlyTitle() {
        TaskDTO updateDTO = new TaskDTO();
        updateDTO.setTitle("新标题");
        
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        
        TaskDTO result = taskService.updateTask(100, updateDTO);
        
        assertNotNull(result);
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    /**
     * Test: Update task - only update content
     */
    @Test
    void testUpdateTask_OnlyContent() {
        TaskDTO updateDTO = new TaskDTO();
        updateDTO.setContent("新内容");
        
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        
        TaskDTO result = taskService.updateTask(100, updateDTO);
        
        assertNotNull(result);
    }

    /**
     * Test: Update task - only update start time
     */
    @Test
    void testUpdateTask_OnlyStartTime() {
        TaskDTO updateDTO = new TaskDTO();
        updateDTO.setStartTime(LocalDateTime.now().plusDays(1));
        
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        
        TaskDTO result = taskService.updateTask(100, updateDTO);
        
        assertNotNull(result);
    }

    /**
     * Test: Update task - only update end time
     */
    @Test
    void testUpdateTask_OnlyEndTime() {
        TaskDTO updateDTO = new TaskDTO();
        updateDTO.setEndTime(LocalDateTime.now().plusDays(7));
        
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        
        TaskDTO result = taskService.updateTask(100, updateDTO);
        
        assertNotNull(result);
    }

    /**
     * Test: Update task - only update priority
     */
    @Test
    void testUpdateTask_OnlyPriority() {
        TaskDTO updateDTO = new TaskDTO();
        updateDTO.setTaskPriority("高");
        
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        
        TaskDTO result = taskService.updateTask(100, updateDTO);
        
        assertNotNull(result);
    }

    /**
     * Test: Update task - only update type
     */
    @Test
    void testUpdateTask_OnlyType() {
        TaskDTO updateDTO = new TaskDTO();
        updateDTO.setTaskType("个人任务");
        
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        
        TaskDTO result = taskService.updateTask(100, updateDTO);
        
        assertNotNull(result);
    }

    /**
     * Test: Update task status without notification (status not changed)
     */
    @Test
    void testUpdateTask_StatusNotChanged() {
        Task oldTask = new Task();
        oldTask.setTaskId(100);
        oldTask.setTaskStatus((byte) 1); // 进行中
        oldTask.setSender(sender);
        
        TaskDTO updateDTO = new TaskDTO();
        updateDTO.setTaskStatus("进行中"); // 相同状态
        
        when(taskRepository.findById(100)).thenReturn(Optional.of(oldTask));
        when(taskRepository.save(any(Task.class))).thenReturn(oldTask);
        
        TaskDTO result = taskService.updateTask(100, updateDTO);
        
        assertNotNull(result);
        // 不应该发送通知
        verify(noticeService, never()).createTaskUpdateNotice(any(), any());
    }

    /**
     * Test: Update task status without sender (should not send notification)
     */
    @Test
    void testUpdateTask_StatusChangedButNoSender() {
        Task oldTask = new Task();
        oldTask.setTaskId(100);
        oldTask.setTaskStatus((byte) 0);
        oldTask.setSender(null); // 没有发送者
        
        TaskDTO updateDTO = new TaskDTO();
        updateDTO.setTaskStatus("进行中");
        
        when(taskRepository.findById(100)).thenReturn(Optional.of(oldTask));
        when(taskRepository.save(any(Task.class))).thenReturn(oldTask);
        
        TaskDTO result = taskService.updateTask(100, updateDTO);
        
        assertNotNull(result);
        verify(noticeService, never()).createTaskUpdateNotice(any(), any());
    }

    /**
     * Test: ConvertToDTO with all null fields
     */
    @Test
    void testConvertToDTO_AllNullFields() {
        Task taskWithNulls = new Task();
        taskWithNulls.setTaskId(999);
        taskWithNulls.setTitle("测试任务");
        taskWithNulls.setTaskStatus(null);
        taskWithNulls.setTaskPriority(null);
        taskWithNulls.setTaskType(null);
        taskWithNulls.setAssignee(null);
        taskWithNulls.setSender(null);
        taskWithNulls.setMilestone(null);
        
        when(taskRepository.findById(999)).thenReturn(Optional.of(taskWithNulls));
        
        TaskDTO result = taskService.getTaskById(999);
        
        assertNotNull(result);
        assertNull(result.getTaskStatus());
        assertNull(result.getTaskPriority());
        assertNull(result.getTaskType());
        assertNull(result.getAssigneeId());
        assertNull(result.getSenderId());
        assertNull(result.getMilestoneId());
    }

    /**
     * Test: Create task without milestone (optional field)
     */
    @Test
    void testCreateTask_WithoutMilestone() {
        taskDTO.setMilestoneId(null);
        
        when(employeeRepository.findById(1)).thenReturn(Optional.of(sender));
        when(employeeRepository.findById(2)).thenReturn(Optional.of(assignee));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        
        TaskDTO result = taskService.createTask(taskDTO);
        
        assertNotNull(result);
        verify(milestoneRepository, never()).findById(anyInt());
    }

    /**
     * Test: Create task with null status (should skip status setting)
     */
    @Test
    void testCreateTask_WithNullStatus() {
        taskDTO.setTaskStatus(null);
        
        when(employeeRepository.findById(1)).thenReturn(Optional.of(sender));
        when(employeeRepository.findById(2)).thenReturn(Optional.of(assignee));
        when(milestoneRepository.findById(10)).thenReturn(Optional.of(milestone));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        
        TaskDTO result = taskService.createTask(taskDTO);
        
        assertNotNull(result);
    }

    /**
     * Test: Create task with null priority (should skip priority setting)
     */
    @Test
    void testCreateTask_WithNullPriority() {
        taskDTO.setTaskPriority(null);
        
        when(employeeRepository.findById(1)).thenReturn(Optional.of(sender));
        when(employeeRepository.findById(2)).thenReturn(Optional.of(assignee));
        when(milestoneRepository.findById(10)).thenReturn(Optional.of(milestone));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        
        TaskDTO result = taskService.createTask(taskDTO);
        
        assertNotNull(result);
    }

    /**
     * Test: Create task with null task type (should skip type setting)
     */
    @Test
    void testCreateTask_WithNullTaskType() {
        taskDTO.setTaskType(null);
        
        when(employeeRepository.findById(1)).thenReturn(Optional.of(sender));
        when(employeeRepository.findById(2)).thenReturn(Optional.of(assignee));
        when(milestoneRepository.findById(10)).thenReturn(Optional.of(milestone));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        
        TaskDTO result = taskService.createTask(taskDTO);
        
        assertNotNull(result);
    }

    /**
     * Test: Create important task (should broadcast notification)
     */
    @Test
    void testCreateTask_ImportantTask() {
        Task importantTask = new Task();
        importantTask.setTaskId(100);
        importantTask.setTitle("重要任务");
        importantTask.setTaskType((byte) 4); // IMPORTANT_TASK code
        importantTask.setSender(sender);
        importantTask.setAssignee(assignee);
        importantTask.setMilestone(milestone);
        
        when(employeeRepository.findById(1)).thenReturn(Optional.of(sender));
        when(employeeRepository.findById(2)).thenReturn(Optional.of(assignee));
        when(milestoneRepository.findById(10)).thenReturn(Optional.of(milestone));
        when(taskRepository.save(any(Task.class))).thenReturn(importantTask);
        
        TaskDTO result = taskService.createTask(taskDTO);
        
        assertNotNull(result);
        verify(noticeService, times(1)).createImportantTaskNotice(any(Task.class));
        verify(noticeService, never()).createTaskAssignmentNotice(any(Task.class));
    }

    /**
     * Test: Update task with milestone change
     */
    @Test
    void testUpdateTask_ChangeMilestone() {
        Milestone newMilestone = new Milestone();
        newMilestone.setMilestoneId(20);
        newMilestone.setTitle("新里程碑");
        
        TaskDTO updateDTO = new TaskDTO();
        updateDTO.setTitle("更新标题");
        updateDTO.setSenderId(1);
        updateDTO.setAssigneeId(2);
        updateDTO.setMilestoneId(20);
        
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(employeeRepository.findById(1)).thenReturn(Optional.of(sender));
        when(employeeRepository.findById(2)).thenReturn(Optional.of(assignee));
        when(milestoneRepository.findById(20)).thenReturn(Optional.of(newMilestone));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        
        TaskDTO result = taskService.updateTask(100, updateDTO);
        
        assertNotNull(result);
        verify(milestoneRepository, times(1)).findById(20);
    }

    /**
     * Test: Update task without milestone
     */
    @Test
    void testUpdateTask_WithoutMilestone() {
        TaskDTO updateDTO = new TaskDTO();
        updateDTO.setTitle("更新标题");
        updateDTO.setSenderId(1);
        updateDTO.setAssigneeId(2);
        updateDTO.setMilestoneId(null);
        
        when(taskRepository.findById(100)).thenReturn(Optional.of(task));
        when(employeeRepository.findById(1)).thenReturn(Optional.of(sender));
        when(employeeRepository.findById(2)).thenReturn(Optional.of(assignee));
        when(taskRepository.save(any(Task.class))).thenReturn(task);
        
        TaskDTO result = taskService.updateTask(100, updateDTO);
        
        assertNotNull(result);
        verify(milestoneRepository, never()).findById(anyInt());
    }

    /**
     * Test: Update task status triggers notification
     */
    @Test
    void testUpdateTask_StatusChangeTriggersNotification() {
        Task oldTask = new Task();
        oldTask.setTaskId(100);
        oldTask.setTaskStatus((byte) 0); // 未开始
        oldTask.setSender(sender);
        oldTask.setAssignee(assignee);
        
        TaskDTO updateDTO = new TaskDTO();
        updateDTO.setTitle("更新标题");
        updateDTO.setTaskStatus("进行中");
        updateDTO.setSenderId(1);
        updateDTO.setAssigneeId(2);
        
        when(taskRepository.findById(100)).thenReturn(Optional.of(oldTask));
        when(employeeRepository.findById(1)).thenReturn(Optional.of(sender));
        when(employeeRepository.findById(2)).thenReturn(Optional.of(assignee));
        when(taskRepository.save(any(Task.class))).thenReturn(oldTask);
        
        TaskDTO result = taskService.updateTask(100, updateDTO);
        
        assertNotNull(result);
        verify(noticeService, times(1)).createTaskUpdateNotice(any(Task.class), any(Employee.class));
    }
}
