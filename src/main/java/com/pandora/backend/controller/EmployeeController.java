package com.pandora.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.pandora.backend.dto.*;
import com.pandora.backend.entity.*;
import com.pandora.backend.service.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @PostMapping
    public EmployeeDTO createEmployee(@RequestBody EmployeeDTO dto) {
        return employeeService.createEmployee(dto);
    }

    @GetMapping("/me")
    public EmployeeDTO getEmployee(@RequestAttribute("userId") Integer userId) {
        Employee emp = employeeService.getEmployeeById(userId);
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmployeeName(emp.getEmployeeName());
        dto.setGender(emp.getGender().getDesc());
        dto.setPhone(emp.getPhone());
        dto.setEmail(emp.getEmail());
        dto.setPosition(emp.getPosition());
        return dto;
    }

    // TODO
    @GetMapping("/me/projects")
    public ProjectDTO getAllProjects(@RequestParam String param) {
        ProjectDTO dto = new ProjectDTO();
        return dto;
    }
}
