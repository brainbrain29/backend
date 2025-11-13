package com.pandora.backend.service;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.pandora.backend.repository.TaskRepository;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.repository.EmployeeTeamRepository;
import com.pandora.backend.repository.MilestoneRepository;
import com.pandora.backend.repository.ProjectRepository;
import com.pandora.backend.dto.AssignableEmployeeDTO;
import com.pandora.backend.dto.TaskDTO;
import com.pandora.backend.dto.TaskStatusDTO;
import com.pandora.backend.enums.Status;
import com.pandora.backend.entity.Task;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.Milestone;
import com.pandora.backend.entity.Project;
import com.pandora.backend.enums.Priority;
import com.pandora.backend.enums.TaskType;
import com.pandora.backend.enums.NoticeType;
import com.pandora.backend.enums.PositionEnum;

@Service
public class TaskService {

    private static final Byte POSITION_TEAM_LEADER = 2;
    private static final Byte LEADER_FLAG = 1;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private MilestoneRepository milestoneRepository;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private EmployeeTeamRepository employeeTeamRepository;

    @Autowired
    private ProjectRepository projectRepository;

    // 创建任务
    public TaskDTO createTask(TaskDTO taskDTO) {
        Task task = new Task();
        task.setTitle(taskDTO.getTitle());
        task.setContent(taskDTO.getContent());
        task.setStartTime(taskDTO.getStartTime());
        task.setEndTime(taskDTO.getEndTime());
        if (taskDTO.getTaskStatus() != null) {
            Status s = Status.fromDesc(taskDTO.getTaskStatus());
            task.setTaskStatus((byte) s.getCode());
        }
        if (taskDTO.getTaskPriority() != null) {
            Priority p = Priority.fromDesc(taskDTO.getTaskPriority());
            task.setTaskPriority((byte) p.getCode());
        }
        if (taskDTO.getTaskType() != null) {
            TaskType t = TaskType.fromDesc(taskDTO.getTaskType());
            task.setTaskType((byte) t.getCode());
        }

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
        // 1. 判断是否为“公司重要任务”，如果是，则广播
        if (savedTask.getTaskType() != null && savedTask.getTaskType() == NoticeType.IMPORTANT_TASK.getCode()) {
            noticeService.createImportantTaskNotice(savedTask);
        }
        // 2. 否则，如果只是普通的“新任务派发”
        else if (savedTask.getAssignee() != null) {
            noticeService.createTaskAssignmentNotice(savedTask);
        }
        // ===================================================================

        return convertToDTO(savedTask);
    }

    // 更新任务
    public TaskDTO updateTask(Integer taskId, TaskDTO taskDTO) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Task not found"));
        // 记录更新前的状态，用于后续比较
        Byte oldStatus = task.getTaskStatus();

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
            Status s = Status.fromDesc(taskDTO.getTaskStatus());
            task.setTaskStatus((byte) s.getCode());
        }
        if (taskDTO.getTaskPriority() != null) {
            Priority p = Priority.fromDesc(taskDTO.getTaskPriority());
            task.setTaskPriority((byte) p.getCode());
        }
        if (taskDTO.getTaskType() != null) {
            TaskType t = TaskType.fromDesc(taskDTO.getTaskType());
            task.setTaskType((byte) t.getCode());
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
        // 检查任务状态是否真的发生了变化
        Byte newStatus = updatedTask.getTaskStatus();
        if (newStatus != null && !newStatus.equals(oldStatus)) {
            // 假设更新者就是任务的发送者 (sender)
            // 在实际应用中，这里应该传入当前操作的用户 (updater)
            if (updatedTask.getSender() != null) {
                noticeService.createTaskUpdateNotice(updatedTask, updatedTask.getSender());
            }
        }
        // ===================================================================

        return convertToDTO(updatedTask);
    }

    /**
     * 验证用户是否有权限为指定项目创建任务
     * 只有项目创建者或负责该项目的团队长才有权限
     */
    private void validateTaskCreationPermission(Integer projectId, Integer userId, Byte userPosition) {
        if (projectId == null) {
            throw new IllegalArgumentException("项目ID不能为空");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在"));

        boolean hasPermission = false;

        // 1. 检查是否是项目创建者
        if (project.getSender() != null && project.getSender().getEmployeeId().equals(userId)) {
            hasPermission = true;
        }

        // 2. 检查是否是负责该项目的团队长
        if (project.getTeam() != null && userPosition == 2) {
            // 检查当前用户是否是该项目的团队长
            List<Integer> leaderTeamIds = employeeTeamRepository.findLeaderTeamIds(userId, LEADER_FLAG);
            if (leaderTeamIds.contains(project.getTeam().getTeamId())) {
                hasPermission = true;
            }
        }

        if (!hasPermission) {
            throw new IllegalArgumentException("无权限为该项目创建任务");
        }
    }

    /**
     * 创建任务（带权限验证版本）
     * 用于需要权限验证的场景，如assignTask接口
     */
    public TaskDTO createTaskWithPermission(TaskDTO taskDTO, Integer userId) {
        Employee user = employeeRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // 验证权限
        validateTaskCreationPermission(taskDTO.getProjectId(), userId, user.getPosition());

        return createTask(taskDTO);
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

    // 查询所有任务 (管理员使用)
    public List<TaskDTO> getAllTasks() {
        List<Task> tasks = taskRepository.findAll();
        return tasks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<TaskDTO> searchTasks(String keyword) {
        if (keyword == null) {
            return Collections.emptyList();
        }

        String trimmedKeyword = keyword.trim();
        if (trimmedKeyword.isEmpty()) {
            return Collections.emptyList();
        }

        List<Task> tasks = taskRepository.searchByKeyword(trimmedKeyword);
        return tasks.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 查询用户的所有任务
     * 返回用户创建的或被分配的任务，去重
     * 
     * @param userId 用户ID
     * @return 任务列表
     */
    public List<TaskDTO> getTasksByUserId(Integer userId) {
        // 获取用户创建的任务
        List<Task> createdTasks = taskRepository.findBySenderEmployeeId(userId);

        // 获取用户被分配的任务
        List<Task> assignedTasks = taskRepository.findByAssigneeEmployeeId(userId);

        // 合并并去重
        Set<Integer> taskIds = new HashSet<>();
        List<Task> allTasks = new ArrayList<>();

        for (Task task : createdTasks) {
            if (taskIds.add(task.getTaskId())) {
                allTasks.add(task);
            }
        }

        for (Task task : assignedTasks) {
            if (taskIds.add(task.getTaskId())) {
                allTasks.add(task);
            }
        }

        return allTasks.stream()
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
        dto.setTaskStatus(task.getTaskStatus() != null ? Status.fromCode(task.getTaskStatus()).getDesc() : null);
        dto.setTaskPriority(
                task.getTaskPriority() != null ? Priority.fromCode(task.getTaskPriority()).getDesc() : null);
        dto.setTaskType(task.getTaskType() != null ? TaskType.fromCode(task.getTaskType()).getDesc() : null);

        if (task.getAssignee() != null) {
            dto.setAssigneeId(task.getAssignee().getEmployeeId());
        }
        if (task.getSender() != null) {
            dto.setSenderId(task.getSender().getEmployeeId());
        }
        if (task.getMilestone() != null) {
            dto.setMilestoneId(task.getMilestone().getMilestoneId());
            if (task.getMilestone().getProject() != null) {
                dto.setProjectId(task.getMilestone().getProject().getProjectId());
            }
        }

        return dto;
    }

    private Employee loadLeaderOrThrow(Integer leaderId) {
        return employeeRepository.findById(leaderId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    private void validateLeaderPosition(Employee leader) {
        if (!POSITION_TEAM_LEADER.equals(leader.getPosition())) {
            throw new IllegalArgumentException("仅团队长可以执行该操作");
        }
    }

    public List<TaskDTO> getTasksByTeam(Integer leaderId) {
        List<Integer> teamIds = employeeTeamRepository.findLeaderTeamIds(leaderId, LEADER_FLAG);
        if (teamIds.isEmpty()) {
            return List.of();
        }
        Set<Integer> memberIds = new HashSet<>();
        memberIds.add(leaderId);
        for (Integer teamId : teamIds) {
            List<Employee> mates = employeeTeamRepository.findTeammates(teamId, leaderId);
            for (Employee e : mates) {
                memberIds.add(e.getEmployeeId());
            }
        }
        List<TaskDTO> result = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (Integer mid : memberIds) {
            List<Task> list = taskRepository.findByAssigneeEmployeeId(mid);
            for (Task t : list) {
                if (seen.add(t.getTaskId())) {
                    result.add(convertToDTO(t));
                }
            }
        }
        return result;
    }

    /**
     * 更新任务状态
     * 权限检查: 只有任务负责人、创建者、上级可以更新状态
     */
    public TaskDTO updateTaskStatus(TaskStatusDTO dto, Integer userId, Byte position) {
        if (dto.getTaskId() == null) {
            throw new IllegalArgumentException("任务ID不能为空");
        }
        if (dto.getTaskStatus() == null) {
            throw new IllegalArgumentException("任务状态不能为空");
        }

        Task task = taskRepository.findById(dto.getTaskId())
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        // 权限检查
        boolean hasPermission = false;

        // 1. 是任务负责人
        if (task.getAssignee() != null && task.getAssignee().getEmployeeId().equals(userId)) {
            hasPermission = true;
        }

        // 2. 是任务创建者
        if (task.getSender() != null && task.getSender().getEmployeeId().equals(userId)) {
            hasPermission = true;
        }

        // 3. 是上级 (部门经理或团队长)
        if (position != null && position <= 2) {
            hasPermission = true;
        }

        if (!hasPermission) {
            throw new IllegalArgumentException("无权限更新此任务状态");
        }

        // 更新状态: 将中文描述转换为 code
        Status status = Status.fromDesc(dto.getTaskStatus());
        task.setTaskStatus((byte) status.getCode());
        Task updatedTask = taskRepository.save(task);

        return convertToDTO(updatedTask);
    }

    /**
     * 获取可分配任务的人员列表
     * 基于项目的团队成员进行分配
     * 部门经理: 可以分配给项目团队的团队长 + 员工
     * 团队长: 只能分配给项目团队的员工
     */
    public List<AssignableEmployeeDTO> getAssignableTaskMembers(
            Integer projectId, Integer userId, Byte position) {

        if (projectId == null) {
            throw new IllegalArgumentException("项目ID不能为空");
        }

        // 1. 获取项目信息
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("项目不存在"));

        // 2. 检查项目是否已分配团队 (使用 project.team 字段)
        if (project.getTeam() == null) {
            throw new IllegalArgumentException("该项目尚未分配负责团队");
        }

        Integer teamId = project.getTeam().getTeamId();

        // 3. 根据调用者的职位确定需要查询的职位列表
        List<Byte> positions;
        if (position == 1) {
            // 部门经理: 可以分配给团队长(2) + 员工(3)
            positions = Arrays.asList((byte) 2, (byte) 3);
        } else if (position == 2) {
            // 团队长: 只能分配给员工(3)
            positions = Arrays.asList((byte) 3);
        } else {
            throw new IllegalArgumentException("无权限分配任务");
        }

        // 4. 直接查询符合条件的团队成员 (在数据库层面过滤)
        List<Employee> teamMembers = employeeTeamRepository
                .findTeamMembersByPositions(teamId, positions);

        // 5. 转换为 DTO
        return teamMembers.stream()
                .map(this::toAssignableDTO)
                .collect(Collectors.toList());
    }

    // TODO:检查接口
    /**
     * 分配任务负责人
     * 部门经理: 可以分配给本部门的团队长和员工
     * 团队长: 可以分配给本团队的员工
     */
    public TaskDTO assignTaskMember(Integer taskId, Integer assigneeId, Integer userId, Byte position) {
        if (assigneeId == null) {
            throw new IllegalArgumentException("负责人ID不能为空");
        }

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        Employee assignee = employeeRepository.findById(assigneeId)
                .orElseThrow(() -> new IllegalArgumentException("负责人不存在"));

        Employee currentUser = employeeRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        // 权限检查
        boolean canAssign = false;

        if (position == 1) {
            // 部门经理: 检查负责人是否在本部门
            if (currentUser.getDepartment() != null &&
                    assignee.getDepartment() != null &&
                    currentUser.getDepartment().getOrgId().equals(assignee.getDepartment().getOrgId())) {
                // 只能分配给团队长或员工
                if (assignee.getPosition() == 2 || assignee.getPosition() == 3) {
                    canAssign = true;
                }
            }
        } else if (position == 2) {
            // 团队长: 检查负责人是否在本团队
            List<Integer> teamIds = employeeTeamRepository.findLeaderTeamIds(userId, LEADER_FLAG);

            for (Integer teamId : teamIds) {
                if (employeeTeamRepository.existsByTeamTeamIdAndEmployeeEmployeeId(teamId, assigneeId)) {
                    // 只能分配给员工
                    if (assignee.getPosition() == 3) {
                        canAssign = true;
                        break;
                    }
                }
            }
        }

        if (!canAssign) {
            throw new IllegalArgumentException("无权限分配给该员工");
        }

        // 分配任务
        task.setAssignee(assignee);
        Task updatedTask = taskRepository.save(task);

        // 创建通知
        noticeService.createTaskAssignmentNotice(updatedTask);

        return convertToDTO(updatedTask);
    }

    /**
     * 转换为可分配员工DTO
     * 使用PositionEnum统一管理职位映射
     */
    private AssignableEmployeeDTO toAssignableDTO(Employee employee) {
        AssignableEmployeeDTO dto = new AssignableEmployeeDTO();
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setEmployeeName(employee.getEmployeeName());
        dto.setEmail(employee.getEmail());
        dto.setPosition(employee.getPosition());

        // 使用枚举设置职位名称，避免硬编码
        dto.setPositionName(PositionEnum.getDescriptionByCode(employee.getPosition()));

        if (employee.getDepartment() != null) {
            dto.setOrgId(employee.getDepartment().getOrgId());
            dto.setOrgName(employee.getDepartment().getOrgName());
        }

        return dto;
    }
}
