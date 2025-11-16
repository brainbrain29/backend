package com.pandora.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.dto.EmployeeDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.enums.Gender;

import java.util.List;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    // 创建新员工
    public EmployeeDTO createEmployee(EmployeeDTO dto) {
        Employee emp = new Employee();
        emp.setEmployeeName(dto.getEmployeeName());
        emp.setGender(Gender.fromDesc(dto.getGender())); // DTO 文字 → Enum
        emp.setPhone(dto.getPhone());
        emp.setEmail(dto.getEmail());
        emp.setPosition(dto.getPosition());

        Employee saved = employeeRepository.save(emp); // JPA 自动生成 INSERT

        // 转换为 DTO 返回给前端
        EmployeeDTO result = new EmployeeDTO();
        result.setEmployeeName(saved.getEmployeeName());
        result.setGender(saved.getGender().getDesc()); // Enum → 文字
        result.setPhone(saved.getPhone());
        result.setEmail(saved.getEmail());
        result.setPosition(saved.getPosition());

        return result;
    }

    // 根据 ID 查询员工信息
    public Employee getEmployeeById(Integer id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
    }

    /**
     * 获取员工详情（包含部门信息）
     * 在事务内主动加载 department，避免懒加载异常
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public EmployeeDTO getEmployeeDetails(Integer id) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));
        
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmployeeId(emp.getEmployeeId());
        dto.setEmployeeName(emp.getEmployeeName());
        dto.setGender(emp.getGender().getDesc());
        dto.setPhone(emp.getPhone());
        dto.setEmail(emp.getEmail());
        dto.setPosition(emp.getPosition());
        
        // 在事务内访问懒加载的 department
        if (emp.getDepartment() != null) {
            dto.setOrgId(emp.getDepartment().getOrgId());
            dto.setOrgName(emp.getDepartment().getOrgName());
        }
        
        return dto;
    }

    // 获取所有员工列表
    public List<Employee> getAllEmployees() {
        return employeeRepository.findAll();
    }
}
