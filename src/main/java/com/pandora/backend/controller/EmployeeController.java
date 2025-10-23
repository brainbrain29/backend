package com.pandora.backend.controller;

import com.pandora.backend.dto.EmployeeDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * @GetMapping("/me/projects")
     * public ResponseEntity<List<ProjectDTO>> getCurrentEmployeeProjects(
     * @RequestAttribute("userId") Integer userId) {
     * List<ProjectDTO> projects = projectService.getProjectsByEmployeeId(userId);
     * return ResponseEntity.ok(projects);
     * }
     */
}
