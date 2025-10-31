package com.pandora.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pandora.backend.dto.LogDTO;
import com.pandora.backend.dto.TaskDTO;
import com.pandora.backend.service.LogService;
import com.pandora.backend.service.TaskService;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.entity.Employee;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private LogService logService;

    @Autowired
    private EmployeeRepository employeeRepository;

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

    @PostMapping("/assign")
    public ResponseEntity<?> assignTask(HttpServletRequest request, @RequestBody TaskDTO body) { // TODO: Manager assigns task (position >= 2)
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;
        Employee emp = employeeRepository.findById(userId).orElse(null);
        if (emp == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        if (emp.getPosition() < 2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Permission denied");
        }
        try {
            body.setSenderId(userId);
            TaskDTO created = taskService.createTask(body);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
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
