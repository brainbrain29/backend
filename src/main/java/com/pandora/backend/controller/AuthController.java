package com.pandora.backend.controller;

import com.pandora.backend.dto.LoginDTO;
import com.pandora.backend.dto.TokenPair;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//TODO:当登录token大于2时应提示前面的登录设备退出登录状态
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private EmployeeRepository employeeRepository;

    @PostMapping("/login")
    public ResponseEntity<TokenPair> login(@RequestBody LoginDTO dto) {
        try {
            // 1. 查找用户
            var employee = employeeRepository.findByPhone(dto.getPhone())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 2. 验证密码
            if (!employee.getPassword().equals(dto.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            // 3. 生成 Token（包含 userId 和 position）
            TokenPair tokens = authService.generateTokens(employee);
            return ResponseEntity.ok(tokens);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(@RequestParam String refreshToken) {
        return ResponseEntity.ok(authService.refreshToken(refreshToken));
    }
}
