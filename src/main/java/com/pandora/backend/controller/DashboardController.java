package com.pandora.backend.controller;

import com.pandora.backend.dto.HomepageDashboardDTO;
import com.pandora.backend.dto.ImportantMatterDTO;
import com.pandora.backend.dto.ImportantTaskDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
// import org.springframework.security.core.annotation.AuthenticationPrincipal; // 将来集成 Security 后使用
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private EmployeeRepository employeeRepository;

    @GetMapping
    // @AuthenticationPrincipal Employee currentUser: 将来集成 Spring Security
    // 后，可以这样直接获取当前登录用户
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

    /**
     * Get important matter by ID
     * GET /dashboard/matters/{matterId}
     * 
     * @param matterId the matter ID
     * @return ImportantMatterDTO or 404 if not found
     */
    @GetMapping("/matters/{matterId}")
    public ResponseEntity<?> getImportantMatterById(@PathVariable Integer matterId) {
        ImportantMatterDTO matter = dashboardService.getImportantMatterById(matterId);
        if (matter == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Important matter not found with ID: " + matterId);
        }
        return ResponseEntity.ok(matter);
    }

    /**
     * Get important task by ID
     * GET /dashboard/tasks/{taskId}
     * 
     * @param taskId the task ID
     * @return ImportantTaskDTO or 404 if not found
     */
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<?> getImportantTaskById(@PathVariable Integer taskId) {
        ImportantTaskDTO task = dashboardService.getImportantTaskById(taskId);
        if (task == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Important task not found with ID: " + taskId);
        }
        return ResponseEntity.ok(task);
    }
}