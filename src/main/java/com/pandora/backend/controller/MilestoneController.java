package com.pandora.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.pandora.backend.dto.MilestoneDTO;
import com.pandora.backend.dto.MilestoneCreateDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.service.MilestoneService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/milestone")
public class MilestoneController { // TODO: 项目创建者才能"操控"里程碑

    @Autowired
    private MilestoneService milestoneService;

    @Autowired
    private EmployeeRepository employeeRepository;

    @PostMapping
    public ResponseEntity<?> createMilestone(HttpServletRequest request, @RequestBody MilestoneCreateDTO body) {

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
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only CEO can create milestone");
        }
        try {
            MilestoneDTO created = milestoneService.createMilestone(body);
            return new ResponseEntity<>(created, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
