package com.pandora.backend.controller;

import com.pandora.backend.entity.Employee;
import com.pandora.backend.repository.EmployeeRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * 显示登录页面
     */
    @GetMapping("/admin/login")
    public String loginPage() {
        return "admin/login";
    }

    /**
     * 处理登录请求
     */
    @PostMapping("/admin/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session,
            Model model) {
        
        // 查找用户
        Optional<Employee> employeeOpt = employeeRepository.findByEmail(email);
        
        if (employeeOpt.isEmpty()) {
            model.addAttribute("error", "邮箱或密码错误");
            return "admin/login";
        }
        
        Employee employee = employeeOpt.get();
        
        // 验证密码
        if (!employee.getPassword().equals(password)) {
            model.addAttribute("error", "邮箱或密码错误");
            return "admin/login";
        }
        
        // 验证是否为管理员（position = 0）
        if (employee.getPosition() != 0) {
            model.addAttribute("error", "您没有管理员权限");
            return "admin/login";
        }
        
        // 登录成功，存储session
        session.setAttribute("adminId", employee.getEmployeeId());
        session.setAttribute("adminName", employee.getEmployeeName());
        session.setAttribute("adminEmail", employee.getEmail());
        
        return "redirect:/admin/web/dashboard";
    }

    /**
     * 登出
     */
    @GetMapping("/admin/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/admin/login";
    }
}
