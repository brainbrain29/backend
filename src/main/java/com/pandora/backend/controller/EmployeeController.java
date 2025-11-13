package com.pandora.backend.controller;

import com.pandora.backend.dto.EmployeeDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.enums.Position;
import com.pandora.backend.service.EmployeeService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    // @Autowired
    // private ProjectService projectService;

    @PostMapping
    public ResponseEntity<EmployeeDTO> createEmployee(@RequestBody EmployeeDTO dto) {
        EmployeeDTO created = employeeService.createEmployee(dto);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping("/me")
    public ResponseEntity<EmployeeDTO> getCurrentEmployee(@RequestAttribute("userId") Integer userId) {
        Employee emp = employeeService.getEmployeeById(userId);
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmployeeName(emp.getEmployeeName());
        dto.setGender(emp.getGender().getDesc());
        dto.setPhone(emp.getPhone());
        dto.setEmail(emp.getEmail());
        dto.setPosition(emp.getPosition());
        return ResponseEntity.ok(dto);
    }

    /**
     * 获取可分配的员工列表（领导专用）
     * GET /employees/assignable
     * 权限：position >= 2
     */
    @GetMapping("/assignable")
    public ResponseEntity<?> getAssignableEmployees(HttpServletRequest request) {
        // 获取当前用户ID
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(401).body("未授权");
        }
        Integer currentUserId = (Integer) uidObj;

        // 获取当前用户信息
        Employee currentUser = employeeService.getEmployeeById(currentUserId);
        if (currentUser == null) {
            return ResponseEntity.status(404).body("用户不存在");
        }

        // 验证权限：只有领导（position >= 2）才能查看
        if (currentUser.getPosition() < 2) {
            return ResponseEntity.status(403).body("权限不足，只有领导才能分配任务");
        }

        // 获取所有员工（排除自己）
        List<Employee> allEmployees = employeeService.getAllEmployees();
        List<Map<String, Object>> assignableEmployees = allEmployees.stream()
            .filter(emp -> !emp.getEmployeeId().equals(currentUserId))  // 排除自己
            .map(emp -> {
                Map<String, Object> empMap = new HashMap<>();
                empMap.put("employeeId", emp.getEmployeeId());
                empMap.put("employeeName", emp.getEmployeeName());
                empMap.put("position", emp.getPosition());
                empMap.put("positionName", Position.fromCode(emp.getPosition()).getDescription());
                empMap.put("phone", emp.getPhone());
                empMap.put("email", emp.getEmail());
                return empMap;
            })
            .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("employees", assignableEmployees);
        response.put("total", assignableEmployees.size());
        response.put("message", "获取成功");

        return ResponseEntity.ok(response);
    }

    /**
     * @GetMapping("/me/projects")
     * public ResponseEntity<List<ProjectDTO>> getCurrentEmployeeProjects(
     * @RequestAttribute("userId") Integer userId) {
     * List<ProjectDTO> projects = projectService.getProjectsByEmployeeId(userId);
     * return ResponseEntity.ok(projects);
     * }
     */
}
