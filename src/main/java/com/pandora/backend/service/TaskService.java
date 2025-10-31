package com.pandora.backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.pandora.backend.repository.TaskRepository;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.repository.MilestoneRepository;
import com.pandora.backend.dto.TaskDTO;
import com.pandora.backend.entity.Task;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.Milestone;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private NoticeService noticeService;

    // 创建任务
    public TaskDTO createTask(TaskDTO taskDTO) {
        Task task = new Task();
        task.setTitle(taskDTO.getTitle());
        task.setContent(taskDTO.getContent());
        task.setStartTime(taskDTO.getStartTime());
        task.setEndTime(taskDTO.getEndTime());
        task.setTaskStatus(taskDTO.getTaskStatus());
        task.setTaskPriority(taskDTO.getTaskPriority());
        task.setTaskType(taskDTO.getTaskType());
        task.setCreatedByWho(taskDTO.getCreatedByWho());

        // 设置发送者（必填）
        if (taskDTO.getSenderId() != null) {
            Employee sender = employeeRepository.findById(taskDTO.getSenderId())
                    .orElseThrow(() -> new RuntimeException("Sender not found"));
            task.setSender(sender);
        } else {
            throw new RuntimeException("Sender ID is required");
        }

        // 设置执行者（可选）
        if (taskDTO.getAssigneeId() != null) {
            Employee assignee = employeeRepository.findById(taskDTO.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
            task.setAssignee(assignee);
        }

        // 设置里程碑（可选）
        if (taskDTO.getMilestoneId() != null) {
            Milestone milestone = milestoneRepository.findById(taskDTO.getMilestoneId())
                    .orElseThrow(() -> new RuntimeException("Milestone not found"));
            task.setMilestone(milestone);
        }

        Task savedTask = taskRepository.save(task);
        // 创建通知：当任务有执行者时，给执行者发送一条通知
        noticeService.createTaskAssignmentNotice(savedTask);
        return convertToDTO(savedTask);
    }

    // 更新任务
    public TaskDTO updateTask(Integer taskId, TaskDTO taskDTO) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        if (taskDTO.getTitle() != null) {
            task.setTitle(taskDTO.getTitle());
        }
        if (taskDTO.getContent() != null) {
            task.setContent(taskDTO.getContent());
        }
        if (taskDTO.getStartTime() != null) {
            task.setStartTime(taskDTO.getStartTime());
        }
        if (taskDTO.getEndTime() != null) {
            task.setEndTime(taskDTO.getEndTime());
        }
        if (taskDTO.getTaskStatus() != null) {
            task.setTaskStatus(taskDTO.getTaskStatus());
        }
        if (taskDTO.getTaskPriority() != null) {
            task.setTaskPriority(taskDTO.getTaskPriority());
        }
        if (taskDTO.getTaskType() != null) {
            task.setTaskType(taskDTO.getTaskType());
        }
        if (taskDTO.getCreatedByWho() != null) {
            task.setCreatedByWho(taskDTO.getCreatedByWho());
        }

        // 更新执行者
        if (taskDTO.getAssigneeId() != null) {
            Employee assignee = employeeRepository.findById(taskDTO.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found"));
            task.setAssignee(assignee);
        }

        // 更新发送者
        if (taskDTO.getSenderId() != null) {
            Employee sender = employeeRepository.findById(taskDTO.getSenderId())
                    .orElseThrow(() -> new RuntimeException("Sender not found"));
            task.setSender(sender);
        }

        // 更新里程碑
        if (taskDTO.getMilestoneId() != null) {
            Milestone milestone = milestoneRepository.findById(taskDTO.getMilestoneId())
                    .orElseThrow(() -> new RuntimeException("Milestone not found"));
            task.setMilestone(milestone);
        }

        Task updatedTask = taskRepository.save(task);
        return convertToDTO(updatedTask);
    }

    // 删除任务
    public void deleteTask(Integer taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new RuntimeException("Task not found");
        }
        taskRepository.deleteById(taskId);
    }

    // 根据ID查询任务
    public TaskDTO getTaskById(Integer taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        return convertToDTO(task);
    }

    // 查询所有任务
    public List<TaskDTO> getAllTasks() {
        List<Task> tasks = taskRepository.findAll();
        return tasks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 根据发送者ID查询任务
    public List<TaskDTO> getTasksBySenderId(Integer senderId) {
        List<Task> tasks = taskRepository.findBySenderEmployeeId(senderId);
        return tasks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 根据执行者ID查询任务
    public List<TaskDTO> getTasksByAssigneeId(Integer assigneeId) {
        List<Task> tasks = taskRepository.findByAssigneeEmployeeId(assigneeId);
        return tasks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 根据里程碑ID查询任务
    public List<TaskDTO> getTasksByMilestoneId(Integer milestoneId) {
        List<Task> tasks = taskRepository.findByMilestoneMilestoneId(milestoneId);
        return tasks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 根据任务状态查询
    public List<TaskDTO> getTasksByStatus(Byte taskStatus) {
        List<Task> tasks = taskRepository.findByTaskStatus(taskStatus);
        return tasks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // 将实体转换为DTO
    private TaskDTO convertToDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setTaskId(task.getTaskId());
        dto.setTitle(task.getTitle());
        dto.setContent(task.getContent());
        dto.setStartTime(task.getStartTime());
        dto.setEndTime(task.getEndTime());
        dto.setTaskStatus(task.getTaskStatus());
        dto.setTaskPriority(task.getTaskPriority());
        dto.setTaskType(task.getTaskType());
        dto.setCreatedByWho(task.getCreatedByWho());

        if (task.getAssignee() != null) {
            dto.setAssigneeId(task.getAssignee().getEmployeeId());
        }
        if (task.getSender() != null) {
            dto.setSenderId(task.getSender().getEmployeeId());
        }
        if (task.getMilestone() != null) {
            dto.setMilestoneId(task.getMilestone().getMilestoneId());
        }

        return dto;
    }
}

