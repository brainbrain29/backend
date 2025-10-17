package com.pandora.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.pandora.backend.dto.LoginDTO;
import com.pandora.backend.dto.EmployeeDTO;
import com.pandora.backend.service.AuthService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/auth")
@Tag(name = "用户接口", description = "用户相关的增删改查接口")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public EmployeeDTO login(@RequestBody LoginDTO loginDTO) {
        return authService.login(loginDTO.getPhone(), loginDTO.getPassword());
    }
}
