package com.pandora.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.pandora.backend.dto.AssignableEmployeeDTO;
import com.pandora.backend.dto.AssignDTO;
import com.pandora.backend.dto.LogDTO;
import com.pandora.backend.dto.TaskDTO;
import com.pandora.backend.dto.TaskStatusDTO;
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
     * 自己创建任务(不需要权限)
     * POST /tasks
     */
    @PostMapping
    public ResponseEntity<?> createTask(HttpServletRequest request, @RequestBody TaskDTO taskDTO) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;
        // 创建时禁止客户端传入 taskId
        if (taskDTO.getTaskId() != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("taskId must not be provided when creating");
        }
        if (taskDTO.getSenderId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Sender ID is required");
        }
        if (!userId.equals(taskDTO.getSenderId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Sender mismatch");
        }
        try {
            TaskDTO createdTask = taskService.createTask(taskDTO);
            return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    // 老板创建任务(需要权限)
    // TODO: 验证身份一定是负责团队的团队长或是创建项目的项目经理
    @PostMapping("/assign")
    public ResponseEntity<?> assignTask(HttpServletRequest request, @RequestBody TaskDTO body) {
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
            if (body.getSenderId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Sender ID is required");
            }
            if (!userId.equals(body.getSenderId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Sender mismatch");
            }
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
    public ResponseEntity<?> updateTask(HttpServletRequest request, @PathVariable Integer id,
            @RequestBody TaskDTO taskDTO) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;
        if (taskDTO.getSenderId() != null && !userId.equals(taskDTO.getSenderId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Sender mismatch");
        }
        try {
            TaskDTO updatedTask = taskService.updateTask(id, taskDTO);
            return new ResponseEntity<>(updatedTask, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    // TODO:只传一个任务状态吗,需要做权限检查吗
    // 更新任务状态
    @PutMapping("{taskId}/status")
    public ResponseEntity<TaskDTO> updateTaskStatus(
            @RequestBody TaskStatusDTO dto,
            HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        Byte position = (Byte) request.getAttribute("position");

        // 权限检查: 只有任务负责人、创建者、上级可以更新状态
        TaskDTO updated = taskService.updateTaskStatus(dto, userId, position);
        return ResponseEntity.ok(updated);
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
     * 根据任务ID查询任务
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
     * 查询当前用户的所有任务
     * 返回用户创建的或被分配的任务
     * GET /tasks
     */
    @GetMapping
    public ResponseEntity<?> getAllTasks(HttpServletRequest request) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;

        try {
            List<TaskDTO> tasks = taskService.getTasksByUserId(userId);
            return new ResponseEntity<>(tasks, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
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

    /**
     * 团队长得到团队所有任务的信息
     * GET /tasks/team
     */
    @GetMapping("/team")
    public ResponseEntity<?> getTasksByTeam(HttpServletRequest request) {
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
        List<TaskDTO> tasks = taskService.getTasksByTeam(userId);
        return ResponseEntity.ok(tasks);
    }

    @GetMapping("/assignable-members")
    public ResponseEntity<?> getAssignableTaskMembers(
            @RequestParam Integer projectId, // 依托的项目
            HttpServletRequest request) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;
        Byte position = (Byte) request.getAttribute("position");

        // 只有部门经理(1) 和团队长(2) 可以调用
        if (position == null || position > 2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only department managers and team leaders can assign tasks");
        }

        try {
            List<AssignableEmployeeDTO> members = taskService.getAssignableTaskMembers(projectId, userId, position);
            return new ResponseEntity<>(members, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{taskId}/assign")
    public ResponseEntity<?> assignTaskMember(
            @PathVariable Integer taskId,
            @RequestBody AssignDTO dto,
            HttpServletRequest request) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;
        Byte position = (Byte) request.getAttribute("position");

        // 只有部门经理(1) 和团队长(2) 可以分配任务
        if (position == null || position > 2) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only department managers and team leaders can assign tasks");
        }

        try {
            TaskDTO updated = taskService.assignTaskMember(taskId, dto.getAssigneeId(), userId, position);
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
