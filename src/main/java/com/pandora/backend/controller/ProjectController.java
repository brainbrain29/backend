package com.pandora.backend.controller;

import com.pandora.backend.dto.AssignDTO;
import com.pandora.backend.dto.MilestoneDTO;
import com.pandora.backend.dto.ProjectCreateDTO;
import com.pandora.backend.dto.ProjectDTO;
import com.pandora.backend.dto.TaskDTO;
import com.pandora.backend.dto.TeamAssignmentOptionDTO;
import com.pandora.backend.dto.TeamNameDTO;
import com.pandora.backend.entity.Department;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.repository.TeamRepository;
import com.pandora.backend.service.MilestoneService;
import com.pandora.backend.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/projects")
public class ProjectController { // TODO: 项目创建者才能修改项目,但谁可见项目呢?

    @Autowired
    private ProjectService projectService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private MilestoneService milestoneService;

    @Autowired
    private TeamRepository teamRepository;

    @PostMapping
    public ResponseEntity<?> createProject(HttpServletRequest request, @RequestBody ProjectCreateDTO body) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;
        Employee emp = employeeRepository.findById(userId).orElse(null);
        if (emp == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        if (emp.getPosition() != 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only CEO can create project");
        }
        try {
            ProjectDTO created = projectService.createProject(body, userId);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{projectId}/milestone")
    public List<MilestoneDTO> getMilestonesByProjectId(@PathVariable Integer projectId) {
        return milestoneService.getMilestonesByProjectId(projectId);
    }

    /**
     * 查询当前员工相关的所有项目
     * 根据员工职位返回不同的项目列表:
     * - CEO 和部门经理: 返回自己创建的项目
     * - 团队长和员工: 返回所在团队负责的项目
     */
    @GetMapping
    public ResponseEntity<?> getAllProjects(HttpServletRequest request) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;
        Byte position = (Byte) request.getAttribute("position");

        try {
            List<ProjectDTO> projects = projectService.getAllProjects(userId, position);
            return new ResponseEntity<>(projects, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDTO> getProjectById(@PathVariable Integer id) {
        try {
            ProjectDTO dto = projectService.getProjectById(id);
            return new ResponseEntity<>(dto, HttpStatus.OK);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProject(HttpServletRequest request, @PathVariable Integer id,
            @RequestBody ProjectCreateDTO body) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;
        Employee emp = employeeRepository.findById(userId).orElse(null);
        if (emp == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        if (emp.getPosition() != 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only CEO can update project");
        }
        try {
            ProjectDTO updated = projectService.updateProject(id, body);
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(HttpServletRequest request, @PathVariable Integer id) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;
        Employee emp = employeeRepository.findById(userId).orElse(null);
        if (emp == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        if (emp.getPosition() != 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only CEO can delete project");
        }
        try {
            projectService.deleteProject(id);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/assignable-leaders")
    public ResponseEntity<?> getAssignableProjectLeaders(HttpServletRequest request) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;
        Byte position = (Byte) request.getAttribute("position");

        // 只有 CEO(0) 和部门经理(1) 可以调用
        if (position == null || position > 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only CEO and department managers can assign projects");
        }

        try {
            List<TeamAssignmentOptionDTO> teams = projectService.getAssignableProjectLeaders(userId, position);
            return new ResponseEntity<>(teams, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{projectId}/assign")
    public ResponseEntity<?> assignProjectLeader(
            @PathVariable Integer projectId,
            @RequestBody AssignDTO dto,
            HttpServletRequest request) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;
        Byte position = (Byte) request.getAttribute("position");

        // 只有 CEO(0) 和部门经理(1) 可以分配项目
        if (position == null || position > 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only CEO and department managers can assign projects");
        }

        // 验证 teamId 参数
        if (dto.getTeamId() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("团队ID不能为空");
        }

        try {
            ProjectDTO updated = projectService.assignProjectLeader(projectId, dto.getTeamId(), userId, position);
            return new ResponseEntity<>(updated, HttpStatus.OK);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 获取项目的所有任务
     * 不区分权限,员工和领导都能看到项目的所有任务
     * 包括未完成、待审核、已完成的任务
     * 返回的任务不包含日志信息
     */
    @GetMapping("/{projectId}/tasks")
    public ResponseEntity<?> getAllTasksByProject(@PathVariable Integer projectId) {
        try {
            List<TaskDTO> tasks = projectService.getAllTasksByProject(projectId);
            return new ResponseEntity<>(tasks, HttpStatus.OK);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * 获取团队名称列表
     * - CEO(position=0): 返回所有团队
     * - 部门经理(position=1): 返回本部门的所有团队
     * - 其他权限: 无权调用
     * GET /projects/teams
     */
    @GetMapping("/teams")
    public ResponseEntity<?> getTeamNames(HttpServletRequest request) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }
        Integer userId = (Integer) uidObj;

        Employee emp = employeeRepository.findById(userId).orElse(null);
        if (emp == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        Byte position = emp.getPosition();

        // 权限检查：只有CEO(0)和部门经理(1)可以调用
        if (position == null || position > 1) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only CEO and department managers can access team information");
        }

        try {
            List<TeamNameDTO> teams;

            if (position == 0) {
                // CEO: 返回所有团队
                teams = teamRepository.findAll().stream()
                        .map(team -> new TeamNameDTO(
                                team.getTeamId(),
                                team.getTeamName(),
                                team.getDepartment().getOrgId(),
                                team.getDepartment().getOrgName()))
                        .collect(Collectors.toList());
            } else {
                // 部门经理(position=1): 返回本部门的团队
                Department department = emp.getDepartment();
                if (department == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body("Department manager must belong to a department");
                }

                teams = department.getTeams().stream()
                        .map(team -> new TeamNameDTO(
                                team.getTeamId(),
                                team.getTeamName(),
                                department.getOrgId(),
                                department.getOrgName()))
                        .collect(Collectors.toList());
            }

            return new ResponseEntity<>(teams, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
