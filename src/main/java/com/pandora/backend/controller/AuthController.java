package com.pandora.backend.controller;

import com.pandora.backend.dto.LoginDTO;
import com.pandora.backend.dto.TokenPair;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        try {
            // 1. 查找用户
            var employee = employeeRepository.findByPhone(dto.getPhone())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 2. 验证密码
            if (!employee.getPassword().equals(dto.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Password not match");
            }

            // 3. 生成 Token（包含 userId 和 position）
            TokenPair tokens = authService.generateTokens(employee);
            return ResponseEntity.ok(tokens);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("未知错误");
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }
}
