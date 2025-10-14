package com.pandora.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.pandora.backend.dto.LoginDTO;
import com.pandora.backend.dto.EmployeeDTO;
import com.pandora.backend.service.AuthService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public EmployeeDTO login(@RequestBody LoginDTO loginDTO) {
        return authService.login(loginDTO.getUsername(), loginDTO.getPassword());
    }
}
