package com.pandora.backend.controller;

import com.pandora.backend.dto.LoginDTO;
import com.pandora.backend.dto.TokenPair;
import com.pandora.backend.security.EmployeeSecurityMapper;
import com.pandora.backend.security.PasswordHashService;
import com.pandora.backend.security.PhoneSecurityService;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Map;

// TODO:应该思考是否需要限制登录设备数量,现在每次登录都有新的token
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PhoneSecurityService phoneSecurityService;

    @Autowired
    private EmployeeSecurityMapper employeeSecurityMapper;

    @Autowired
    private PasswordHashService passwordHashService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginDTO dto) {
        if (dto.getPhone() == null || dto.getPhone().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "手机号不能为空"));
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "密码不能为空"));
        }

        String phoneHash = phoneSecurityService.hashPhone(dto.getPhone());
        Optional<com.pandora.backend.entity.Employee> employeeOpt = employeeRepository.findByPhoneHash(phoneHash);
        if (employeeOpt.isEmpty()) {
            employeeOpt = employeeRepository.findByPhoneEnc(dto.getPhone().trim());
        }
        if (employeeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "用户不存在"));
        }

        com.pandora.backend.entity.Employee employee = employeeOpt.get();

        // Verify password
        if (!passwordHashService.matches(dto.getPassword(), employee.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "密码错误"));
        }

        boolean shouldUpgradePassword = employee.getPassword() != null
                && !(employee.getPassword().startsWith("$2a$")
                        || employee.getPassword().startsWith("$2b$")
                        || employee.getPassword().startsWith("$2y$"));

        boolean shouldUpgradePhone = employee.getPhoneHash() == null
                || phoneSecurityService.isLegacyPlainPhoneEnc(employee.getPhoneEnc());

        if (shouldUpgradePassword) {
            employee.setPassword(passwordHashService.hashPassword(dto.getPassword()));
        }

        if (shouldUpgradePhone) {
            employeeSecurityMapper.setPhone(employee, dto.getPhone());
        }

        if (shouldUpgradePassword || shouldUpgradePhone) {
            employeeRepository.save(employee);
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
