package com.pandora.backend.controller;

import com.pandora.backend.dto.ProjectCreateDTO;
import com.pandora.backend.dto.ProjectDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.service.ProjectService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects")
public class ProjectController { // TODO: CEO-only project creation controller

    @Autowired
    private ProjectService projectService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @PostMapping
    public ResponseEntity<?> createProject(HttpServletRequest request, @RequestBody ProjectCreateDTO body) { // TODO: CEO creates project
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
}
