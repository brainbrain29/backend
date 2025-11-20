package com.pandora.backend.service;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pandora.backend.dto.ProjectCreateDTO;
import com.pandora.backend.dto.TaskDTO;
import com.pandora.backend.dto.TeamAssignmentOptionDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.Task;
import com.pandora.backend.enums.Priority;
import com.pandora.backend.enums.Status;
import com.pandora.backend.enums.TaskType;
import com.pandora.backend.entity.Employee_Team;
import com.pandora.backend.entity.Project;
import com.pandora.backend.entity.Team;
import com.pandora.backend.dto.ProjectDTO;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.repository.EmployeeTeamRepository;
import com.pandora.backend.repository.ProjectRepository;
import com.pandora.backend.repository.TaskRepository;
import com.pandora.backend.repository.TeamRepository;

//TODO:权限控制
@Service
public class ProjectService {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EmployeeTeamRepository employeeTeamRepository;

    @Autowired
    private TeamRepository teamRepository;

    @Autowired
    private TaskRepository taskRepository;

    public ProjectDTO createProject(ProjectCreateDTO dto, Integer senderId) {
        Employee sender = employeeRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("Sender not found"));

        Project p = new Project();
        p.setTitle(dto.getTitle());
        p.setContent(dto.getContent());
        p.setStartTime(dto.getStartTime());
        p.setEndTime(dto.getEndTime());

        // 将前端传递的中文描述转换为 code
        if (dto.getProjectStatus() != null) {
            Status status = Status.fromDesc(dto.getProjectStatus());
            p.setProjectStatus((byte) status.getCode());
        } else {
            p.setProjectStatus((byte) Status.NOT_FINISHED.getCode()); // 默认未完成
        }

        p.setSender(sender);

        // 如果提供了 teamId,设置项目的团队
        if (dto.getTeamId() != null) {
            Team team = teamRepository.findById(dto.getTeamId())
                    .orElseThrow(() -> new RuntimeException("团队不存在: " + dto.getTeamId()));
            p.setTeam(team);
        }

        Project saved = projectRepository.save(p);
        return convertToDto(saved);
    }

    /**
     * 查询员工相关的所有项目
     * 根据员工职位返回不同的项目列表:
     * - CEO(0) 和部门经理(1): 返回自己创建的项目
     * - 团队长(2) 和员工(3): 返回所在团队负责的项目
     * 
     * @param employeeId 员工ID
     * @param position   员工职位 (0=CEO, 1=部门经理, 2=团队长, 3=员工)
     * @return 项目列表
     */
    public List<ProjectDTO> getAllProjects(Integer employeeId, Byte position) {
        List<Project> projects;

        // CEO(0) 和部门经理(1): 查询自己创建的项目
        if (position != null && position <= 1) {
            projects = projectRepository.findBySenderEmployeeId(employeeId);
        }
        // 团队长(2) 和员工(3): 查询所在团队负责的项目
        else {
            projects = projectRepository.findProjectsByEmployeeId(employeeId);
        }

        return convertToDtoList(projects);
    }

    /**
     * 查询员工参与的所有项目 (通过团队)
     * 用于团队长和员工
     * 
     * @param employeeId 员工ID
     * @return 员工参与的项目列表
     */
    public List<ProjectDTO> getProjectsByEmployeeId(Integer employeeId) {
        List<Project> projects = projectRepository.findProjectsByEmployeeId(employeeId);
        return convertToDtoList(projects);
    }

    public ProjectDTO getProjectById(Integer id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        return convertToDto(project);
    }

    public ProjectDTO updateProject(Integer id, ProjectCreateDTO dto) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        if (dto.getTitle() != null) {
            project.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            project.setContent(dto.getContent());
        }
        if (dto.getStartTime() != null) {
            project.setStartTime(dto.getStartTime());
        }
        if (dto.getEndTime() != null) {
            project.setEndTime(dto.getEndTime());
        }
        if (dto.getProjectStatus() != null) {
            Status status = Status.fromDesc(dto.getProjectStatus());
            project.setProjectStatus((byte) status.getCode());
        }

        Project saved = projectRepository.save(project);
        return convertToDto(saved);
    }

    public void deleteProject(Integer id) {
        if (!projectRepository.existsById(id)) {
            throw new RuntimeException("Project not found");
        }
        projectRepository.deleteById(id);
    }

    private List<ProjectDTO> convertToDtoList(List<Project> projects) {
        return projects.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private ProjectDTO convertToDto(Project project) {
        ProjectDTO dto = new ProjectDTO();
        dto.setProjectId(project.getProjectId());
        dto.setTitle(project.getTitle());
        dto.setContent(project.getContent());
        dto.setStartTime(project.getStartTime());
        dto.setEndTime(project.getEndTime());

        // 将 code 转换为中文描述返回给前端
        if (project.getProjectStatus() != null) {
            Status status = Status.fromCode(project.getProjectStatus());
            dto.setProjectStatus(status.getDesc());
        }

        // 创建者信息
        if (project.getSender() != null) {
            dto.setSenderId(project.getSender().getEmployeeId());
            dto.setSenderName(project.getSender().getEmployeeName());
        }

        // 团队信息
        if (project.getTeam() != null) {
            dto.setTeamId(project.getTeam().getTeamId());
            dto.setTeamName(project.getTeam().getTeamName());
        }

        return dto;
    }

    /**
     * 获取可分配项目的团队列表
     * CEO: 所有团队
     * 部门经理: 本部门的团队
     */
    public List<TeamAssignmentOptionDTO> getAssignableProjectLeaders(Integer userId, Byte position) {
        List<Employee_Team> teamLeaderRelations;

        if (position == 0) {
            // CEO: 查询所有团队
            teamLeaderRelations = employeeTeamRepository.findAllTeamsWithLeaders((byte) 1);
        } else if (position == 1) {
            // 部门经理: 查询本部门的团队
            Employee manager = employeeRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            if (manager.getDepartment() == null) {
                throw new IllegalArgumentException("部门经理未关联部门");
            }

            teamLeaderRelations = employeeTeamRepository.findTeamsByDepartment(
                    manager.getDepartment().getOrgId(), (byte) 1);
        } else {
            throw new IllegalArgumentException("无权限分配项目");
        }

        // 转换为 DTO
        return teamLeaderRelations.stream()
                .map(this::toTeamAssignmentDTO)
                .collect(Collectors.toList());
    }

    /**
     * 分配项目负责团队
     * CEO: 可以分配给所有团队
     * 部门经理: 只能分配给本部门的团队
     * 
     * 优化: 直接查询指定团队的团队长,减少数据库查询
     */
    public ProjectDTO assignProjectLeader(Integer projectId, Integer teamId, Integer userId, Byte position) {
        if (teamId == null) {
            throw new IllegalArgumentException("团队ID不能为空");
        }

        // 1. 查询项目
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("项目不存在"));

        // 2. 查询团队
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("团队不存在"));

        // 3. 直接查询该团队的团队长 (优化: 只查询一个团队)
        Employee leader = employeeTeamRepository.findTeamLeader(teamId, (byte) 1);
        if (leader == null) {
            throw new IllegalArgumentException("该团队没有团队长");
        }

        // 4. 权限检查
        if (position == 1) {
            // 部门经理: 检查团队是否在本部门
            Employee manager = employeeRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

            if (manager.getDepartment() == null) {
                throw new IllegalArgumentException("用户未关联部门");
            }

            if (team.getDepartment() == null ||
                    !manager.getDepartment().getOrgId().equals(team.getDepartment().getOrgId())) {
                throw new IllegalArgumentException("无权限分配给该团队");
            }
        }
        // CEO (position == 0) 不需要权限检查

        // 5. 分配项目负责团队和团队长
        project.setTeam(team);
        project.setSender(leader);
        Project updatedProject = projectRepository.save(project);

        return convertToDto(updatedProject);
    }

    /**
     * 转换为团队分配DTO
     */
    private TeamAssignmentOptionDTO toTeamAssignmentDTO(Employee_Team employeeTeam) {
        TeamAssignmentOptionDTO dto = new TeamAssignmentOptionDTO();

        // 团队信息
        dto.setTeamId(employeeTeam.getTeam().getTeamId());
        dto.setTeamName(employeeTeam.getTeam().getTeamName());

        return dto;
    }

    /**
     * 获取项目的所有任务
     * 不区分权限,所有员工和领导都能看到项目的所有任务
     * 返回的任务不包含日志信息
     * 
     * @param projectId 项目ID
     * @return 任务列表
     */
    public List<TaskDTO> getAllTasksByProject(Integer projectId) {
        // 验证项目是否存在
        if (!projectRepository.existsById(projectId)) {
            throw new RuntimeException("项目不存在");
        }

        // 查询项目的所有任务
        List<Task> tasks = taskRepository.findByProjectId(projectId);

        // 转换为DTO(不包含日志信息)
        return tasks.stream()
                .map(this::convertTaskToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 将Task实体转换为TaskDTO(不包含日志信息)
     */
    private TaskDTO convertTaskToDTO(Task task) {
        TaskDTO dto = new TaskDTO();
        dto.setTaskId(task.getTaskId());
        dto.setTitle(task.getTitle());
        dto.setContent(task.getContent());
        dto.setStartTime(task.getStartTime());
        dto.setEndTime(task.getEndTime());

        // 转换任务状态
        if (task.getTaskStatus() != null) {
            Status status = Status.fromCode(task.getTaskStatus());
            dto.setTaskStatus(status.getDesc());
        }

        // 转换任务优先级
        if (task.getTaskPriority() != null) {
            Priority priority = Priority.fromCode(task.getTaskPriority());
            dto.setTaskPriority(priority.getDesc());
        }

        // 负责人信息
        if (task.getAssignee() != null) {
            dto.setAssigneeId(task.getAssignee().getEmployeeId());
            dto.setAssigneeName(task.getAssignee().getEmployeeName());
        }

        // 创建者信息
        if (task.getSender() != null) {
            dto.setSenderId(task.getSender().getEmployeeId());
            dto.setSenderName(task.getSender().getEmployeeName());
        }

        // 转换任务类型
        if (task.getTaskType() != null) {
            TaskType taskType = TaskType.fromCode(task.getTaskType());
            dto.setTaskType(taskType.getDesc());
        }

        // 里程碑信息
        if (task.getMilestone() != null) {
            dto.setMilestoneId(task.getMilestone().getMilestoneId());
            dto.setMilestoneName(task.getMilestone().getTitle());

            // 项目信息(通过里程碑获取)
            if (task.getMilestone().getProject() != null) {
                dto.setProjectId(task.getMilestone().getProject().getProjectId());
                dto.setProjectName(task.getMilestone().getProject().getTitle());
            }
        }

        // 注意: 不设置日志信息
        return dto;
    }
}
