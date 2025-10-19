package com.pandora.backend.controller;

import com.pandora.backend.dto.LoginDTO;
import com.pandora.backend.dto.TokenPair;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private EmployeeRepository employeeRepository;

    @PostMapping("/login")
    public TokenPair login(@RequestBody LoginDTO dto) {
        return authService.generateTokens(
                employeeRepository.findByPhone(dto.getPhone())
                        .orElseThrow(() -> new RuntimeException("User not found")));
    }

    @PostMapping("/refresh")
    public TokenPair refresh(@RequestParam String refreshToken) {
        return authService.refreshToken(refreshToken);
    }
}
