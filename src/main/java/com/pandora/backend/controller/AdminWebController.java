package com.pandora.backend.controller;

import com.pandora.backend.dto.DepartmentDTO;
import com.pandora.backend.dto.EmployeeDTO;
import com.pandora.backend.dto.TeamDTO;
import com.pandora.backend.dto.ImportantMatterDTO;
import com.pandora.backend.dto.ImportantTaskDTO;
import com.pandora.backend.service.AdminService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/admin/web")
public class AdminWebController {

    @Autowired
    private AdminService adminService;

    @GetMapping
    public String index() {
        return "redirect:/admin/web/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        List<ImportantMatterDTO> matters = adminService.getAllImportantMatters();
        List<ImportantTaskDTO> tasks = adminService.getAllImportantTasks();
        List<EmployeeDTO> employees = adminService.getAllEmployees();
        List<DepartmentDTO> departments = adminService.getAllDepartments();
        List<TeamDTO> teams = adminService.getAllTeams();
        
        // 添加系统统计数据和活动日志
        model.addAttribute("matters", matters);
        model.addAttribute("tasks", tasks);
        model.addAttribute("employees", employees);
        model.addAttribute("departments", departments);
        model.addAttribute("teams", teams);
        model.addAttribute("stats", adminService.getSystemStats());
        model.addAttribute("activities", adminService.getRecentActivities());
        model.addAttribute("teamForm", new TeamDTO());
        return "admin/dashboard";
    }

    @GetMapping("/profile")
    public String profile(Model model, HttpSession session) {
        Integer adminId = (Integer) session.getAttribute("adminId");
        if (adminId != null) {
            var admin = adminService.getAdminById(adminId);
            model.addAttribute("profileForm", admin);
        }
        return "admin/dashboard";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String adminName,
                                @RequestParam String adminEmail,
                                @RequestParam(required = false) String adminPhone,
                                @RequestParam(required = false) String newPassword,
                                @RequestParam(required = false) String confirmPassword,
                                @RequestParam(required = false) MultipartFile avatar,
                                HttpSession session,
                                Model model) {
        Integer adminId = (Integer) session.getAttribute("adminId");
        if (adminId == null) {
            return "redirect:/admin/login";
        }

        if (newPassword != null && !newPassword.equals(confirmPassword)) {
            model.addAttribute("profileError", "两次输入的密码不一致");
            return "redirect:/admin/web/dashboard";
        }

        try {
            var updated = adminService.updateAdminProfile(adminId, adminName, adminEmail, adminPhone, newPassword, avatar);
            session.setAttribute("adminName", updated.getEmployeeName());
            session.setAttribute("adminEmail", updated.getEmail());
            session.setAttribute("adminPhone", updated.getPhone());
            session.setAttribute("adminAvatar", updated.getAvatarUrl());
            model.addAttribute("profileMessage", "个人信息已更新");
        } catch (IllegalArgumentException ex) {
            model.addAttribute("profileError", ex.getMessage());
        } catch (RuntimeException ex) {
            model.addAttribute("profileError", ex.getMessage());
        }

        return "redirect:/admin/web/dashboard";
    }

    @GetMapping("/employees")
    public String employees(Model model) {
        List<EmployeeDTO> employees = adminService.getAllEmployees();
        List<DepartmentDTO> departments = adminService.getAllDepartments();
        model.addAttribute("employees", employees);
        model.addAttribute("departments", departments);
        model.addAttribute("employeeForm", new EmployeeDTO());
        return "admin/employees";
    }

    @PostMapping("/employees")
    public String createEmployee(@ModelAttribute("employeeForm") EmployeeDTO dto) {
        adminService.createEmployee(dto);
        return "redirect:/admin/web/dashboard#employee-section";
    }

    @PostMapping("/employees/{id}")
    public String updateEmployee(@PathVariable Integer id, @ModelAttribute("employeeForm") EmployeeDTO dto) {
        adminService.updateEmployee(id, dto);
        return "redirect:/admin/web/dashboard#employee-section";
    }

    @PostMapping("/employees/{id}/transfer")
    public String transfer(@PathVariable Integer id, @RequestParam(required = false) Integer departmentId) {
        adminService.transferDepartment(id, departmentId);
        return "redirect:/admin/web/dashboard#employee-section";
    }

    @PostMapping("/employees/{id}/delete")
    public String deleteEmployee(@PathVariable Integer id) {
        adminService.deleteEmployee(id);
        return "redirect:/admin/web/dashboard#employee-section";
    }

    @PostMapping("/teams")
    public String createTeam(@RequestParam String teamName,
                             @RequestParam Integer orgId,
                             @RequestParam(required = false) Integer leaderId,
                             @RequestParam(value = "memberIds", required = false) List<Integer> memberIds) {
        TeamDTO dto = new TeamDTO();
        dto.setTeamName(teamName);
        dto.setOrgId(orgId);
        dto.setLeaderId(leaderId);
        dto.setMemberIds(memberIds);
        adminService.createTeam(dto);
        return "redirect:/admin/web/dashboard#team-section";
    }

    @PostMapping("/teams/{id}")
    public String updateTeam(@PathVariable Integer id,
                             @RequestParam String teamName,
                             @RequestParam Integer orgId,
                             @RequestParam(required = false) Integer leaderId,
                             @RequestParam(value = "memberIds", required = false) List<Integer> memberIds) {
        TeamDTO dto = new TeamDTO();
        dto.setTeamName(teamName);
        dto.setOrgId(orgId);
        dto.setLeaderId(leaderId);
        dto.setMemberIds(memberIds);
        adminService.updateTeam(id, dto);
        return "redirect:/admin/web/dashboard#team-section";
    }

    @PostMapping("/teams/{id}/delete")
    public String deleteTeam(@PathVariable Integer id) {
        adminService.deleteTeam(id);
        return "redirect:/admin/web/dashboard#team-section";
    }

    @GetMapping("/departments")
    public String departments(Model model) {
        List<DepartmentDTO> departments = adminService.getAllDepartments();
        model.addAttribute("departments", departments);
        return "admin/departments";
    }

    @GetMapping("/important-matters")
    public String importantMatters(Model model) {
        List<ImportantMatterDTO> matters = adminService.getAllImportantMatters();
        List<EmployeeDTO> employees = adminService.getAllEmployees();
        model.addAttribute("matters", matters);
        model.addAttribute("employees", employees);
        model.addAttribute("matterForm", new ImportantMatterDTO());
        return "admin/important-matters";
    }

    @PostMapping("/important-matters")
    public String createImportantMatter(@ModelAttribute("matterForm") ImportantMatterDTO dto) {
        adminService.createImportantMatter(dto);
        return "redirect:/admin/web/dashboard#matter-section";
    }

    @PostMapping("/important-matters/{id}")
    public String updateImportantMatter(@PathVariable Integer id, @ModelAttribute("matterForm") ImportantMatterDTO dto) {
        adminService.updateImportantMatter(id, dto);
        return "redirect:/admin/web/dashboard#matter-section";
    }

    @PostMapping("/important-matters/{id}/delete")
    public String deleteImportantMatter(@PathVariable Integer id) {
        adminService.deleteImportantMatter(id);
        return "redirect:/admin/web/dashboard#matter-section";
    }

    @GetMapping("/important-person-tasks")
    public String importantTasks(Model model) {
        List<ImportantTaskDTO> tasks = adminService.getAllImportantTasks();
        List<EmployeeDTO> employees = adminService.getAllEmployees();
        model.addAttribute("tasks", tasks);
        model.addAttribute("employees", employees);
        model.addAttribute("taskForm", new ImportantTaskDTO());
        return "admin/important-person-tasks";
    }

    @PostMapping("/important-person-tasks")
    public String createImportantTask(@ModelAttribute("taskForm") ImportantTaskDTO dto) {
        adminService.createImportantTask(dto);
        return "redirect:/admin/web/dashboard#task-section";
    }

    @PostMapping("/important-person-tasks/{id}")
    public String updateImportantTask(@PathVariable Integer id, @ModelAttribute("taskForm") ImportantTaskDTO dto) {
        adminService.updateImportantTask(id, dto);
        return "redirect:/admin/web/dashboard#task-section";
    }

    @PostMapping("/important-person-tasks/{id}/delete")
    public String deleteImportantTask(@PathVariable Integer id) {
        adminService.deleteImportantTask(id);
        return "redirect:/admin/web/dashboard#task-section";
    }
}
