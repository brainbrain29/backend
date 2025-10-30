package com.pandora.backend.controller;

import com.pandora.backend.dto.HomepageDashboardDTO;
import com.pandora.backend.entity.Employee; // 导入你的 Employee 实体
import com.pandora.backend.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/home")
    // @AuthenticationPrincipal Employee currentUser: 将来集成 Spring Security 后，可以这样直接获取当前登录用户
    public ResponseEntity<HomepageDashboardDTO> getHomepageDashboard() {
        // 为了方便当前测试，我们先写死一个用户ID，比如 ID 为 1 的用户
        Integer currentUserId = 1;

        HomepageDashboardDTO dashboardData = dashboardService.getDashboardData(currentUserId);
        return ResponseEntity.ok(dashboardData);
    }
}