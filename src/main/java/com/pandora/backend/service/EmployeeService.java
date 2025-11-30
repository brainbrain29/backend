package com.pandora.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.repository.LogRepository;
import com.pandora.backend.repository.TaskRepository;
import com.pandora.backend.dto.DailyWorkloadDTO;
import com.pandora.backend.dto.EmployeeDTO;
import com.pandora.backend.dto.EmployeeWeeklyStatsDTO;
import com.pandora.backend.dto.MoodStatisticsDTO;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.Log;
import com.pandora.backend.entity.Task;
import com.pandora.backend.enums.Emoji;
import com.pandora.backend.enums.Gender;
import com.pandora.backend.enums.Position;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private LogRepository logRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 创建新员工
    public EmployeeDTO createEmployee(EmployeeDTO dto) {
        Employee emp = new Employee();
        emp.setEmployeeName(dto.getEmployeeName());
        emp.setGender(Gender.fromDesc(dto.getGender())); // DTO 文字 → Enum
        emp.setPhone(dto.getPhone());
        emp.setEmail(dto.getEmail());
        emp.setPosition(dto.getPosition());
        emp.setMbti(dto.getMbti());

        Employee saved = employeeRepository.save(emp); // JPA 自动生成 INSERT

        // 转换为 DTO 返回给前端
        EmployeeDTO result = new EmployeeDTO();
        result.setEmployeeName(saved.getEmployeeName());
        result.setGender(saved.getGender().getDesc()); // Enum → 文字
        result.setPhone(saved.getPhone());
        result.setEmail(saved.getEmail());
        result.setPosition(saved.getPosition());
        result.setMbti(saved.getMbti());

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
        dto.setPositionName(Position.getDescriptionByCode(emp.getPosition()));
        dto.setMbti(emp.getMbti());

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

    /**
     * 更新员工信息
     */
    public Employee updateEmployee(Employee employee) {
        return employeeRepository.save(employee);
    }

    /**
     * 修改员工密码
     *
     * @param employeeId  员工ID
     * @param newPassword 新密码
     */
    public void changePassword(Integer employeeId, String newPassword) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("员工不存在"));

        // 验证新密码与当前密码是否相同
        if (employee.getPassword().equals(newPassword)) {
            throw new RuntimeException("新密码不能与当前密码相同");
        }

        employee.setPassword(newPassword);
        employeeRepository.save(employee);
    }

    /**
     * 获取员工本周统计数据（带 Redis 缓存）
     * 
     * @param employeeId    员工ID
     * @param referenceDate 参考日期（用于计算本周，null 则使用当前日期）
     * @return 本周统计数据
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public EmployeeWeeklyStatsDTO getWeeklyStats(Integer employeeId, LocalDate referenceDate) {
        // 如果未提供参考日期，使用当前日期
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }

        // 计算本周的开始和结束日期（周一到周日）
        LocalDate weekStart = referenceDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = referenceDate.with(DayOfWeek.SUNDAY);

        // 生成 Redis 缓存 key
        String cacheKey = "employee:weekly:stats:" + employeeId + ":" + weekStart;

        // 尝试从 Redis 获取缓存
        EmployeeWeeklyStatsDTO cachedStats = (EmployeeWeeklyStatsDTO) redisTemplate.opsForValue().get(cacheKey);
        if (cachedStats != null) {
            System.out.println("✅ 从 Redis 获取员工本周统计，员工ID: " + employeeId);
            return cachedStats;
        }

        System.out.println("⚠️ Redis 未命中，从数据库查询员工本周统计，员工ID: " + employeeId);

        // 查询员工信息
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        // 转换为 LocalDateTime 用于数据库查询
        LocalDateTime weekStartTime = weekStart.atStartOfDay();
        LocalDateTime weekEndTime = weekEnd.atTime(23, 59, 59);

        // 1. 统计本周日志数
        List<Log> weeklyLogs = logRepository.findAll().stream()
                .filter(log -> log.getEmployee() != null && log.getEmployee().getEmployeeId().equals(employeeId))
                .filter(log -> log.getCreatedTime() != null)
                .filter(log -> !log.getCreatedTime().isBefore(weekStartTime)
                        && !log.getCreatedTime().isAfter(weekEndTime))
                .toList();
        int logCount = weeklyLogs.size();

        // 2. 统计本周任务（包括完成和未完成）
        // 注意：Task 实体使用 startTime 作为任务创建时间
        List<Task> weeklyTasks = taskRepository.findAll().stream()
                .filter(task -> task.getAssignee() != null && task.getAssignee().getEmployeeId().equals(employeeId))
                .filter(task -> task.getStartTime() != null)
                .filter(task -> !task.getStartTime().isBefore(weekStartTime)
                        && !task.getStartTime().isAfter(weekEndTime))
                .toList();

        int totalTaskCount = weeklyTasks.size();
        int completedTaskCount = (int) weeklyTasks.stream()
                .filter(task -> task.getTaskStatus() != null && task.getTaskStatus() == 2) // 2 = 已完成
                .count();

        // 3. 计算任务完成率
        double completionRate = 0.0;
        if (totalTaskCount > 0) {
            completionRate = (double) completedTaskCount / totalTaskCount * 100;
            completionRate = Math.round(completionRate * 100.0) / 100.0; // 保留两位小数
        }

        // 构建统计 DTO
        EmployeeWeeklyStatsDTO stats = new EmployeeWeeklyStatsDTO();
        stats.setEmployeeId(employeeId);
        stats.setEmployeeName(employee.getEmployeeName());
        stats.setWeekStart(weekStart);
        stats.setWeekEnd(weekEnd);
        stats.setLogCount(logCount);
        stats.setCompletedTaskCount(completedTaskCount);
        stats.setTotalTaskCount(totalTaskCount);
        stats.setCompletionRate(completionRate);

        // 存入 Redis 缓存（缓存 1 小时）
        redisTemplate.opsForValue().set(cacheKey, stats, 1, TimeUnit.HOURS);
        System.out.println("✅ 员工本周统计已缓存到 Redis，员工ID: " + employeeId);

        return stats;
    }

    /**
     * 获取员工一周心情统计（带 Redis 缓存）
     * 
     * @param employeeId    员工ID
     * @param referenceDate 参考日期（用于计算本周，null 则使用当前日期）
     * @return 心情统计数据，包含各种心情的数量和占比
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public MoodStatisticsDTO getWeeklyMoodStatistics(Integer employeeId, LocalDate referenceDate) {
        // 如果未提供参考日期，使用当前日期
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }

        // 计算本周的开始和结束日期（周一到周日）
        LocalDate weekStart = referenceDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = referenceDate.with(DayOfWeek.SUNDAY);

        // 生成 Redis 缓存 key
        String cacheKey = "employee:weekly:mood:" + employeeId + ":" + weekStart;

        // 尝试从 Redis 获取缓存
        MoodStatisticsDTO cachedStats = (MoodStatisticsDTO) redisTemplate.opsForValue().get(cacheKey);
        if (cachedStats != null) {
            System.out.println("✅ 从 Redis 获取员工心情统计，员工ID: " + employeeId);
            return cachedStats;
        }

        System.out.println("⚠️ Redis 未命中，从数据库查询员工心情统计，员工ID: " + employeeId);

        // 查询员工信息
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        // 转换为 LocalDateTime 用于数据库查询
        LocalDateTime weekStartTime = weekStart.atStartOfDay();
        LocalDateTime weekEndTime = weekEnd.atTime(23, 59, 59);

        // 查询本周的所有日志
        List<Log> weeklyLogs = logRepository.findAll().stream()
                .filter(log -> log.getEmployee() != null && log.getEmployee().getEmployeeId().equals(employeeId))
                .filter(log -> log.getCreatedTime() != null)
                .filter(log -> !log.getCreatedTime().isBefore(weekStartTime)
                        && !log.getCreatedTime().isAfter(weekEndTime))
                .toList();

        int totalLogs = weeklyLogs.size();

        // 统计每种心情的数量
        Map<String, Integer> moodCounts = new HashMap<>();
        for (Emoji emoji : Emoji.values()) {
            moodCounts.put(emoji.getDesc(), 0);
        }

        for (Log log : weeklyLogs) {
            if (log.getEmoji() != null) {
                String moodDesc = log.getEmoji().getDesc();
                moodCounts.put(moodDesc, moodCounts.get(moodDesc) + 1);
            }
        }

        // 计算每种心情的占比
        Map<String, MoodStatisticsDTO.MoodCount> moodDistribution = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
            int count = entry.getValue();
            double percentage = totalLogs > 0 ? (double) count / totalLogs * 100 : 0.0;
            percentage = Math.round(percentage * 100.0) / 100.0; // 保留两位小数
            moodDistribution.put(entry.getKey(), new MoodStatisticsDTO.MoodCount(count, percentage));
        }

        // 构建统计 DTO
        MoodStatisticsDTO stats = new MoodStatisticsDTO();
        stats.setEmployeeId(employeeId);
        stats.setEmployeeName(employee.getEmployeeName());
        stats.setWeekStart(weekStart);
        stats.setWeekEnd(weekEnd);
        stats.setTotalLogs(totalLogs);
        stats.setMoodDistribution(moodDistribution);

        // 存入 Redis 缓存（缓存 1 小时）
        redisTemplate.opsForValue().set(cacheKey, stats, 1, TimeUnit.HOURS);
        System.out.println("✅ 员工心情统计已缓存到 Redis，员工ID: " + employeeId);

        return stats;
    }

    /**
     * 获取员工一周每日工作量（带 Redis 缓存）
     * 
     * @param employeeId    员工ID
     * @param referenceDate 参考日期（用于计算本周，null 则使用当前日期）
     * @return 每日工作量数据，包含每天的日志数和任务数
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public DailyWorkloadDTO getWeeklyDailyWorkload(Integer employeeId, LocalDate referenceDate) {
        // 如果未提供参考日期，使用当前日期
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }

        // 计算本周的开始和结束日期（周一到周日）
        LocalDate weekStart = referenceDate.with(DayOfWeek.MONDAY);
        LocalDate weekEnd = referenceDate.with(DayOfWeek.SUNDAY);

        // 生成 Redis 缓存 key
        String cacheKey = "employee:weekly:workload:" + employeeId + ":" + weekStart;

        // 尝试从 Redis 获取缓存
        DailyWorkloadDTO cachedData = (DailyWorkloadDTO) redisTemplate.opsForValue().get(cacheKey);
        if (cachedData != null) {
            System.out.println("✅ 从 Redis 获取员工每日工作量，员工ID: " + employeeId);
            return cachedData;
        }

        System.out.println("⚠️ Redis 未命中，从数据库查询员工每日工作量，员工ID: " + employeeId);

        // 查询员工信息
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + employeeId));

        // 转换为 LocalDateTime 用于数据库查询
        LocalDateTime weekStartTime = weekStart.atStartOfDay();
        LocalDateTime weekEndTime = weekEnd.atTime(23, 59, 59);

        // 查询本周的所有日志和任务
        List<Log> weeklyLogs = logRepository.findAll().stream()
                .filter(log -> log.getEmployee() != null && log.getEmployee().getEmployeeId().equals(employeeId))
                .filter(log -> log.getCreatedTime() != null)
                .filter(log -> !log.getCreatedTime().isBefore(weekStartTime)
                        && !log.getCreatedTime().isAfter(weekEndTime))
                .toList();

        List<Task> weeklyTasks = taskRepository.findAll().stream()
                .filter(task -> task.getAssignee() != null && task.getAssignee().getEmployeeId().equals(employeeId))
                .filter(task -> task.getStartTime() != null)
                .filter(task -> !task.getStartTime().isBefore(weekStartTime)
                        && !task.getStartTime().isAfter(weekEndTime))
                .toList();

        // 按日期分组统计
        Map<LocalDate, Long> logCountByDate = weeklyLogs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getCreatedTime().toLocalDate(),
                        Collectors.counting()));

        Map<LocalDate, Long> taskCountByDate = weeklyTasks.stream()
                .collect(Collectors.groupingBy(
                        task -> task.getStartTime().toLocalDate(),
                        Collectors.counting()));

        // 构建一周7天的每日数据
        List<DailyWorkloadDTO.DailyData> dailyDataList = new ArrayList<>();
        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            int logCount = logCountByDate.getOrDefault(date, 0L).intValue();
            int taskCount = taskCountByDate.getOrDefault(date, 0L).intValue();
            String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            dailyDataList.add(new DailyWorkloadDTO.DailyData(date, dayOfWeek, logCount, taskCount));
        }

        // 构建工作量 DTO
        DailyWorkloadDTO workload = new DailyWorkloadDTO();
        workload.setEmployeeId(employeeId);
        workload.setEmployeeName(employee.getEmployeeName());
        workload.setWeekStart(weekStart);
        workload.setWeekEnd(weekEnd);
        workload.setDailyData(dailyDataList);

        // 存入 Redis 缓存（缓存 1 小时）
        redisTemplate.opsForValue().set(cacheKey, workload, 1, TimeUnit.HOURS);
        System.out.println("✅ 员工每日工作量已缓存到 Redis，员工ID: " + employeeId);

        return workload;
    }
}
