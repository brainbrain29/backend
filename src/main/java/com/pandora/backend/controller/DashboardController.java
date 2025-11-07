package com.pandora.backend.controller;

import com.pandora.backend.dto.HomepageDashboardDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.annotation.AuthenticationPrincipal; // 将来集成 Security 后使用
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping("/home")
    // @AuthenticationPrincipal Employee currentUser: 将来集成 Spring Security 后，可以这样直接获取当前登录用户
    public ResponseEntity<?> getHomepageDashboard(HttpServletRequest request) {
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid token");
        }

        Integer currentUserId = (Integer) uidObj;
        Employee employee = employeeRepository.findById(currentUserId).orElse(null);
        if (employee == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        HomepageDashboardDTO dashboardData = dashboardService.getDashboardData(currentUserId);
        return ResponseEntity.ok(dashboardData);
    }
}