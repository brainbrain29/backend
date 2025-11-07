package com.pandora.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pandora.backend.dto.LogDTO;
import com.pandora.backend.dto.TaskDTO;
import com.pandora.backend.service.LogService;
import com.pandora.backend.service.TaskService;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private LogService logService;

    /**
     * 创建任务
     * POST /tasks
     */
    @PostMapping
    public ResponseEntity<TaskDTO> createTask(@RequestBody TaskDTO taskDTO) {
        try {
            TaskDTO createdTask = taskService.createTask(taskDTO);
            return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 更新任务
     * PUT /tasks/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<TaskDTO> updateTask(@PathVariable Integer id, @RequestBody TaskDTO taskDTO) {
        try {
            TaskDTO updatedTask = taskService.updateTask(id, taskDTO);
            return new ResponseEntity<>(updatedTask, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 删除任务
     * DELETE /tasks/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Integer id) {
        try {
            taskService.deleteTask(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * 根据ID查询任务
     * GET /tasks/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TaskDTO> getTaskById(@PathVariable Integer id) {
        try {
            TaskDTO task = taskService.getTaskById(id);
            return new ResponseEntity<>(task, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    /**
     * 查询所有任务
     * GET /tasks
     */
    @GetMapping
    public ResponseEntity<List<TaskDTO>> getAllTasks() {
        List<TaskDTO> tasks = taskService.getAllTasks();
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    @GetMapping("/search")
    public ResponseEntity<List<TaskDTO>> searchTasks(@RequestParam("keyword") String keyword) {
        List<TaskDTO> tasks = taskService.searchTasks(keyword);
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    /**
     * 根据发送者ID查询任务
     * GET /tasks/sender/{senderId}
     */
    @GetMapping("/sender/{senderId}")
    public ResponseEntity<List<TaskDTO>> getTasksBySenderId(@PathVariable Integer senderId) {
        List<TaskDTO> tasks = taskService.getTasksBySenderId(senderId);
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    /**
     * 根据执行者ID查询任务
     * GET /tasks/assignee/{assigneeId}
     */
    @GetMapping("/assignee/{assigneeId}")
    public ResponseEntity<List<TaskDTO>> getTasksByAssigneeId(@PathVariable Integer assigneeId) {
        List<TaskDTO> tasks = taskService.getTasksByAssigneeId(assigneeId);
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    /**
     * 根据里程碑ID查询任务
     * GET /tasks/milestone/{milestoneId}
     */
    @GetMapping("/milestone/{milestoneId}")
    public ResponseEntity<List<TaskDTO>> getTasksByMilestoneId(@PathVariable Integer milestoneId) {
        List<TaskDTO> tasks = taskService.getTasksByMilestoneId(milestoneId);
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    /**
     * 根据任务状态查询
     * GET /tasks/status/{taskStatus}
     */
    @GetMapping("/status/{taskStatus}")
    public ResponseEntity<List<TaskDTO>> getTasksByStatus(@PathVariable Byte taskStatus) {
        List<TaskDTO> tasks = taskService.getTasksByStatus(taskStatus);
        return new ResponseEntity<>(tasks, HttpStatus.OK);
    }

    // 根据任务ID获取该任务所有日志
    @GetMapping("/{taskId}/logs")
    public ResponseEntity<List<LogDTO>> getLogsByTask(@PathVariable("taskId") Integer taskId) {
        List<LogDTO> logs = logService.getLogsByTask(taskId);
        return ResponseEntity.ok(logs);
    }
}
