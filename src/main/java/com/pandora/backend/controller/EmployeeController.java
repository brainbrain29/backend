package com.pandora.backend.controller;

import com.pandora.backend.dto.ChangePasswordDTO;
import com.pandora.backend.dto.DailyWorkloadDTO;
import com.pandora.backend.dto.EmployeeDTO;
import com.pandora.backend.dto.EmployeeWeeklyStatsDTO;
import com.pandora.backend.dto.ImportantMatterDTO;
import com.pandora.backend.dto.ImportantTaskDTO;
import com.pandora.backend.dto.MoodStatisticsDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.enums.Position;
import com.pandora.backend.service.EmployeeService;
import com.pandora.backend.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private DashboardService dashboardService;

    // @Autowired
    // private ProjectService projectService;

    /**
     * @PostMapping
     *              public ResponseEntity<EmployeeDTO> createEmployee(@RequestBody
     *              EmployeeDTO dto) {
     *              EmployeeDTO created = employeeService.createEmployee(dto);
     *              return ResponseEntity.status(201).body(created);
     *              }
     */

    @GetMapping("/me")
    public ResponseEntity<EmployeeDTO> getCurrentEmployee(@RequestAttribute("userId") Integer userId) {
        EmployeeDTO dto = employeeService.getEmployeeDetails(userId);
        return ResponseEntity.ok(dto);
    }

    /**
     * 获取可分配的员工列表（领导专用）
     * GET /employees/assignable
     * 权限：position >= 2
     */
    @GetMapping("/assignable")
    public ResponseEntity<?> getAssignableEmployees(HttpServletRequest request) {
        // 获取当前用户ID
        Object uidObj = request.getAttribute("userId");
        if (uidObj == null) {
            return ResponseEntity.status(401).body("未授权");
        }
        Integer currentUserId = (Integer) uidObj;

        // 获取当前用户信息
        Employee currentUser = employeeService.getEmployeeById(currentUserId);
        if (currentUser == null) {
            return ResponseEntity.status(404).body("用户不存在");
        }

        // 验证权限：只有领导（position >= 2）才能查看
        if (currentUser.getPosition() < 2) {
            return ResponseEntity.status(403).body("权限不足，只有领导才能分配任务");
        }

        // 获取所有员工（排除自己）
        List<Employee> allEmployees = employeeService.getAllEmployees();
        List<Map<String, Object>> assignableEmployees = allEmployees.stream()
                .filter(emp -> !emp.getEmployeeId().equals(currentUserId)) // 排除自己
                .map(emp -> {
                    Map<String, Object> empMap = new HashMap<>();
                    empMap.put("employeeId", emp.getEmployeeId());
                    empMap.put("employeeName", emp.getEmployeeName());
                    empMap.put("position", emp.getPosition());
                    empMap.put("positionName", Position.getDescriptionByCode(emp.getPosition()));
                    empMap.put("phone", emp.getPhone());
                    empMap.put("email", emp.getEmail());
                    return empMap;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("employees", assignableEmployees);
        response.put("total", assignableEmployees.size());
        response.put("message", "获取成功");

        return ResponseEntity.ok(response);
    }

    /**
     * 修改当前用户密码
     * PUT /employees/me/password
     *
     * @param userId 当前用户ID（从 JWT Token 中获取）
     * @param dto    包含旧密码和新密码的 DTO
     * @return 修改结果
     */
    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(
            @RequestAttribute("userId") Integer userId,
            @RequestBody ChangePasswordDTO dto) {

        if (dto.getNewPassword() == null || dto.getNewPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "新密码不能为空"));
        }

        try {
            employeeService.changePassword(userId, dto.getNewPassword());
            return ResponseEntity.ok(Map.of("message", "密码修改成功"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 更新当前用户的信息
     * PUT /employees/me
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateEmployee(
            @RequestAttribute("userId") Integer userId,
            @RequestBody EmployeeDTO dto) {

        Employee employee = employeeService.getEmployeeById(userId);

        // 更新允许修改的字段
        if (dto.getEmployeeName() != null && !dto.getEmployeeName().trim().isEmpty()) {
            employee.setEmployeeName(dto.getEmployeeName());
        }

        if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
            employee.setPhone(dto.getPhone());
        }

        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            employee.setEmail(dto.getEmail());
        }

        if (dto.getMbti() != null && !dto.getMbti().trim().isEmpty()) {
            // 验证 MBTI 格式（应该是4个大写字母）
            if (!dto.getMbti().matches("^[A-Z]{4}$")) {
                return ResponseEntity.badRequest().body("MBTI 格式不正确，应为4个大写字母（如INTJ）");
            }
            employee.setMbti(dto.getMbti());
        }

        employeeService.updateEmployee(employee);

        // 返回更新后的员工信息
        EmployeeDTO updatedDto = employeeService.getEmployeeDetails(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "员工信息更新成功");
        response.put("employee", updatedDto);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取员工本周统计数据
     * GET /employees/weekly-stats?date={date}
     * 
     * @param userId 当前用户ID（从 JWT Token 中获取）
     * @param date   参考日期（可选，格式：yyyy-MM-dd，默认为当前日期）
     * @return 本周统计数据
     */
    @GetMapping("/weekly-stats")
    public ResponseEntity<?> getWeeklyStats(
            @RequestAttribute("userId") Integer userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            EmployeeWeeklyStatsDTO stats = employeeService.getWeeklyStats(userId, date);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 根据 ID 查询重要事项详情
     * GET /employees/important-matters/{matterId}
     * 
     * @param matterId 重要事项ID
     * @return 重要事项详情
     *         @GetMapping("/important-matters/{matterId}")
     *         public ResponseEntity<?> getImportantMatterById(@PathVariable Integer
     *         matterId) {
     *         try {
     *         ImportantMatterDTO matter =
     *         dashboardService.getImportantMatterById(matterId);
     *         if (matter == null) {
     *         return ResponseEntity.status(404).body(Map.of("error", "重要事项未找到，ID: "
     *         + matterId));
     *         }
     *         return ResponseEntity.ok(matter);
     *         } catch (Exception e) {
     *         return ResponseEntity.badRequest().body(Map.of("error",
     *         e.getMessage()));
     *         }
     *         }
     * 
     *         /**
     *         根据 ID 查询重要任务详情
     *         GET /employees/important-tasks/{taskId}
     * 
     * @param taskId 重要任务ID
     * @return 重要任务详情
     *         @GetMapping("/important-tasks/{taskId}")
     *         public ResponseEntity<?> getImportantTaskById(@PathVariable Integer
     *         taskId) {
     *         try {
     *         ImportantTaskDTO task =
     *         dashboardService.getImportantTaskById(taskId);
     *         if (task == null) {
     *         return ResponseEntity.status(404).body(Map.of("error", "重要任务未找到，ID: "
     *         + taskId));
     *         }
     *         return ResponseEntity.ok(task);
     *         } catch (Exception e) {
     *         return ResponseEntity.badRequest().body(Map.of("error",
     *         e.getMessage()));
     *         }
     *         }
     */

    /**
     * 获取员工一周心情统计
     * GET /employees/weekly-mood-statistics?date={date}
     * 
     * @param userId 当前用户ID（从 JWT Token 中获取）
     * @param date   参考日期（可选，格式：yyyy-MM-dd，默认为当前日期）
     * @return 一周心情统计数据，包含各种心情的数量和占比
     */
    @GetMapping("/weekly-mood-statistics")
    public ResponseEntity<?> getWeeklyMoodStatistics(
            @RequestAttribute("userId") Integer userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            MoodStatisticsDTO stats = employeeService.getWeeklyMoodStatistics(userId, date);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /**
     * 获取员工一周每日工作量
     * GET /employees/weekly-daily-workload?date={date}
     * 
     * @param userId 当前用户ID（从 JWT Token 中获取）
     * @param date   参考日期（可选，格式：yyyy-MM-dd，默认为当前日期）
     * @return 一周每日工作量数据，包含每天的日志数和任务数
     */
    @GetMapping("/weekly-daily-workload")
    public ResponseEntity<?> getWeeklyDailyWorkload(
            @RequestAttribute("userId") Integer userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        try {
            DailyWorkloadDTO workload = employeeService.getWeeklyDailyWorkload(userId, date);
            return ResponseEntity.ok(workload);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
