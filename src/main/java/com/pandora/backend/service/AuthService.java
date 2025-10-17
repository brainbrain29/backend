package com.pandora.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import com.pandora.backend.dto.EmployeeDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.repository.EmployeeRepository;

@Service
public class AuthService {

    @Autowired
    private EmployeeRepository employeeRepository;

    public EmployeeDTO login(String phone, String password) {
        // 查询员工
        Employee emp = employeeRepository.findByPhone(phone);

        if (emp == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在");
        }

        if (!emp.getPassword().equals(password)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "密码错误");
        }

        // 转 DTO 返回
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmployeeName(emp.getEmployeeName());
        dto.setEmail(emp.getEmail());
        dto.setGender(emp.getGender().getDesc());
        dto.setPosition(emp.getPosition());
        dto.setPhone(emp.getPhone());
        return dto;
    }
}
