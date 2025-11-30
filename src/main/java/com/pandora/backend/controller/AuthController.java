package com.pandora.backend.controller;

import com.pandora.backend.dto.LoginDTO;
import com.pandora.backend.dto.TokenPair;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

// TODO:应该思考是否需要限制登录设备数量,现在每次登录都有新的token
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private EmployeeRepository employeeRepository;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO dto) {
        // Validate input
        if (dto.getPhone() == null || dto.getPhone().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "手机号不能为空"));
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "密码不能为空"));
        }

        // Find user by phone
        var employeeOpt = employeeRepository.findByPhone(dto.getPhone());
        if (employeeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "用户不存在"));
        }

        var employee = employeeOpt.get();

        // Verify password
        if (!employee.getPassword().equals(dto.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "密码错误"));
        }

        // 3. 生成 Token（包含 userId 和 position）
        TokenPair tokens = authService.generateTokens(employee);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }
}
