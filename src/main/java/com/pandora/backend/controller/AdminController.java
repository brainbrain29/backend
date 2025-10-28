package com.pandora.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.pandora.backend.dto.*;
import com.pandora.backend.service.AdminService;

import java.util.List;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    // ========== 员工管理 ==========

    /**
     * 获取所有员工
     */
    @GetMapping("/employees")
    public ResponseEntity<List<EmployeeDTO>> getAllEmployees() {
        List<EmployeeDTO> employees = adminService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    /**
     * 获取单个员工
     */
    @GetMapping("/employees/{id}")
    public ResponseEntity<EmployeeDTO> getEmployee(@PathVariable Integer id) {
        List<EmployeeDTO> employees = adminService.getAllEmployees();
        EmployeeDTO employee = employees.stream()
                .filter(emp -> emp.getEmployeeId().equals(id))
                .findFirst()
                .orElse(null);
        if (employee != null) {
            return ResponseEntity.ok(employee);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * 创建新员工
     */
    @PostMapping("/employees")
    public ResponseEntity<EmployeeDTO> createEmployee(@RequestBody EmployeeDTO dto) {
        try {
            EmployeeDTO result = adminService.createEmployee(dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新员工信息
     */
    @PutMapping("/employees/{id}")
    public ResponseEntity<EmployeeDTO> updateEmployee(@PathVariable Integer id, @RequestBody EmployeeDTO dto) {
        try {
            EmployeeDTO result = adminService.updateEmployee(id, dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 部门调动
     */
    @PutMapping("/employees/{id}/transfer")
    public ResponseEntity<Void> transferDepartment(@PathVariable Integer id, @RequestParam Integer departmentId) {
        try {
            adminService.transferDepartment(id, departmentId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除员工（离职）
     */
    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Integer id) {
        try {
            adminService.deleteEmployee(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 获取所有部门
     */
    @GetMapping("/departments")
    public ResponseEntity<List<DepartmentDTO>> getAllDepartments() {
        List<DepartmentDTO> departments = adminService.getAllDepartments();
        return ResponseEntity.ok(departments);
    }

    // ========== 十大重要事项管理 ==========

    /**
     * 获取所有重要事项
     */
    @GetMapping("/important-matters")
    public ResponseEntity<List<ImportantMatterDTO>> getAllImportantMatters() {
        List<ImportantMatterDTO> matters = adminService.getAllImportantMatters();
        return ResponseEntity.ok(matters);
    }

    /**
     * 创建重要事项
     */
    @PostMapping("/important-matters")
    public ResponseEntity<ImportantMatterDTO> createImportantMatter(@RequestBody ImportantMatterDTO dto) {
        try {
            ImportantMatterDTO result = adminService.createImportantMatter(dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新重要事项
     */
    @PutMapping("/important-matters/{id}")
    public ResponseEntity<ImportantMatterDTO> updateImportantMatter(@PathVariable Integer id, @RequestBody ImportantMatterDTO dto) {
        try {
            ImportantMatterDTO result = adminService.updateImportantMatter(id, dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除重要事项
     */
    @DeleteMapping("/important-matters/{id}")
    public ResponseEntity<Void> deleteImportantMatter(@PathVariable Integer id) {
        try {
            adminService.deleteImportantMatter(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ========== 十大重要人任务管理 ==========

    /**
     * 获取所有重要人任务
     */
    @GetMapping("/important-person-tasks")
    public ResponseEntity<List<ImportantPersonTaskDTO>> getAllImportantPersonTasks() {
        List<ImportantPersonTaskDTO> tasks = adminService.getAllImportantPersonTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * 创建重要人任务
     */
    @PostMapping("/important-person-tasks")
    public ResponseEntity<ImportantPersonTaskDTO> createImportantPersonTask(@RequestBody ImportantPersonTaskDTO dto) {
        try {
            ImportantPersonTaskDTO result = adminService.createImportantPersonTask(dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新重要人任务
     */
    @PutMapping("/important-person-tasks/{id}")
    public ResponseEntity<ImportantPersonTaskDTO> updateImportantPersonTask(@PathVariable Integer id, @RequestBody ImportantPersonTaskDTO dto) {
        try {
            ImportantPersonTaskDTO result = adminService.updateImportantPersonTask(id, dto);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 删除重要人任务
     */
    @DeleteMapping("/important-person-tasks/{id}")
    public ResponseEntity<Void> deleteImportantPersonTask(@PathVariable Integer id) {
        try {
            adminService.deleteImportantPersonTask(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}

