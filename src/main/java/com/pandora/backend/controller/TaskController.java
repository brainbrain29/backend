package com.pandora.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.pandora.backend.dto.AssignableEmployeeDTO;
import com.pandora.backend.dto.AssignDTO;
import com.pandora.backend.dto.LogDTO;
import com.pandora.backend.dto.TaskDTO;
import com.pandora.backend.dto.TaskStatusDTO;
import com.pandora.backend.service.AttachmentService;
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

    @Autowired
    private AttachmentService attachmentService;

    /**
     * 自己创建任务(不需要权限)
     * POST /tasks
     * 支持文件上传，使用 multipart/form-data
     */
    @PostMapping
    public ResponseEntity<?> createTask(
            HttpServletRequest request,
            @RequestParam(value = "title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "startTime", required = false) String startTime,
            @RequestParam(value = "endTime", required = false) String endTime,
            @RequestParam(value = "taskPriority", required = false) String taskPriority,
            @RequestParam(value = "taskType", required = false) String taskType,
            @RequestParam(value = "assigneeId", required = false) Integer assigneeId,
            @RequestParam(value = "milestoneId", required = false) Integer milestoneId,
            @RequestParam(value = "files", required = false) MultipartFile[] files) {

        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;

        try {
            // 构建 TaskDTO
            TaskDTO taskDTO = new TaskDTO();
            taskDTO.setTitle(title);
            taskDTO.setContent(content);
            taskDTO.setSenderId(userId);
            taskDTO.setAssigneeId(assigneeId);
            taskDTO.setMilestoneId(milestoneId);
            taskDTO.setTaskPriority(taskPriority);
            taskDTO.setTaskType(taskType);

            // 解析时间
            if (startTime != null && !startTime.isEmpty()) {
                taskDTO.setStartTime(java.time.LocalDateTime.parse(startTime));
            }
            if (endTime != null && !endTime.isEmpty()) {
                taskDTO.setEndTime(java.time.LocalDateTime.parse(endTime));
            }

            // 创建任务并处理附件
            TaskDTO createdTask = taskService.createTaskWithAttachments(taskDTO, files, userId);
            return new ResponseEntity<>(createdTask, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("创建任务失败: " + e.getMessage());
        }
    }

    // 为项目创建任务（需要权限）
    // 权限要求：必须是项目创建者或负责该项目的团队长
    @PostMapping("/assign")
    public ResponseEntity<?> assignTask(HttpServletRequest request, @RequestBody TaskDTO body) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;

        try {
            // 验证输入参数
            if (body.getSenderId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Sender ID is required");
            }
            if (!userId.equals(body.getSenderId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Sender mismatch");
            }
            if (body.getMilestoneId() == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Milestone ID is required");
            }

            // 验证用户身份
            Employee emp = employeeRepository.findById(userId).orElse(null);
            if (emp == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            // 执行权限检查和创建任务
            TaskDTO created = taskService.createTaskWithPermission(body, userId);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
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
            TaskDTO updatedTask = taskService.updateTask(id, taskDTO, userId);
            return new ResponseEntity<>(updatedTask, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    // TODO:更改url为/status更合理
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
    public ResponseEntity<List<TaskDTO>> searchTasks(
            HttpServletRequest request,
            @RequestParam(value = "keyword", required = false) String keyword) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        Integer userId = (Integer) uidObj;

        List<TaskDTO> tasks = taskService.searchTasks(keyword, userId);
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
     * 查询当前员工的未完成任务
     * 从 token 获取员工 ID，返回状态为"未开始"(0)和"进行中"(1)的任务
     * GET /tasks/status
     */
    @GetMapping("/status")
    public ResponseEntity<List<TaskDTO>> getTasksByStatus(HttpServletRequest request) {
        // 从 token 中获取员工 ID
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        Integer userId = (Integer) userIdObj;

        // 获取该员工的未完成任务（状态 0: 未开始, 1: 进行中）
        List<TaskDTO> tasks = taskService.getUnfinishedTasksByEmployeeId(userId);
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
        Object positionObj = request.getAttribute("position");
        Byte position = (Byte) positionObj;

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

    /**
     * 接收任务
     * 将任务状态从"未接收"改为"未完成"
     * PUT /tasks/{taskId}/accept
     */
    @PutMapping("/{taskId}/accept")
    public ResponseEntity<?> acceptTask(
            HttpServletRequest request,
            @PathVariable Integer taskId) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;

        try {
            TaskDTO task = taskService.acceptTask(taskId, userId);
            return new ResponseEntity<>(task, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("服务器错误");
        }
    }

    /**
     * 下载任务附件
     * GET /tasks/attachments/{id}/download
     */
    @GetMapping("/attachments/{id}/download")
    public ResponseEntity<Resource> downloadTaskAttachment(
            @PathVariable("id") Long attachmentId,
            @RequestParam(value = "token", required = false) String tokenParam,
            HttpServletRequest request) {
        return attachmentService.downloadTaskAttachment(attachmentId, tokenParam, request);
    }

    /**
     * 预览任务附件
     * GET /tasks/attachments/{id}/preview
     */
    @GetMapping("/attachments/{id}/preview")
    public ResponseEntity<Resource> previewTaskAttachment(
            @PathVariable("id") Long attachmentId,
            @RequestParam(value = "token", required = false) String tokenParam,
            HttpServletRequest request) {
        return attachmentService.previewTaskAttachment(attachmentId, tokenParam, request);
    }
}
