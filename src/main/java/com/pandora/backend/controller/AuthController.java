package com.pandora.backend.controller;

import com.pandora.backend.dto.TokenPair;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private EmployeeRepository employeeRepository;

    @PostMapping("/login")
    public TokenPair login(@RequestParam String phone, @RequestParam String password) {
        Optional<Employee> empOpt = employeeRepository.findByPhone(phone);
        Employee emp = empOpt.orElseThrow(() -> new RuntimeException("User not found"));
        return authService.generateTokens(emp);
    }

    @PostMapping("/refresh")
    public TokenPair refresh(@RequestParam String refreshToken) {
        return authService.refreshToken(refreshToken);
    }
}
