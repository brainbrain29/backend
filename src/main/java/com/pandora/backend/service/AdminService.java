package com.pandora.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pandora.backend.dto.*;
import com.pandora.backend.entity.*;
import com.pandora.backend.repository.*;
import com.pandora.backend.enums.Gender;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ImportantMatterRepository importantMatterRepository;

    @Autowired
    private ImportantTaskRepository importantTaskRepository;

    // ========== 员工管理 ==========
    
    /**
     * 获取所有员工列表
     */
    public List<EmployeeDTO> getAllEmployees() {
        List<Employee> employees = employeeRepository.findAll();
        return employees.stream()
                .map(emp -> {
                    EmployeeDTO dto = new EmployeeDTO();
                    dto.setEmployeeId(emp.getEmployeeId());
                    dto.setEmployeeName(emp.getEmployeeName());
                    dto.setGender(emp.getGender().getDesc());
                    dto.setPhone(emp.getPhone());
                    dto.setEmail(emp.getEmail());
                    dto.setPosition(emp.getPosition());
                    if (emp.getDepartment() != null) {
                        dto.setOrgId(emp.getDepartment().getOrgId());
                        dto.setOrgName(emp.getDepartment().getOrgName());
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 创建新员工
     */
    @Transactional
    public EmployeeDTO createEmployee(EmployeeDTO dto) {
        Employee emp = new Employee();
        emp.setEmployeeName(dto.getEmployeeName());
        emp.setGender(Gender.fromDesc(dto.getGender()));
        emp.setPhone(dto.getPhone());
        emp.setEmail(dto.getEmail());
        emp.setPosition(dto.getPosition());
        
        // 设置默认密码
        emp.setPassword("123456");
        
        // 设置部门
        if (dto.getOrgId() != null) {
            Department department = departmentRepository.findById(dto.getOrgId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            emp.setDepartment(department);
        }

        Employee saved = employeeRepository.save(emp);

        EmployeeDTO result = new EmployeeDTO();
        result.setEmployeeId(saved.getEmployeeId());
        result.setEmployeeName(saved.getEmployeeName());
        result.setGender(saved.getGender().getDesc());
        result.setPhone(saved.getPhone());
        result.setEmail(saved.getEmail());
        result.setPosition(saved.getPosition());
        if (saved.getDepartment() != null) {
            result.setOrgId(saved.getDepartment().getOrgId());
            result.setOrgName(saved.getDepartment().getOrgName());
        }

        return result;
    }

    /**
     * 更新员工信息
     */
    @Transactional
    public EmployeeDTO updateEmployee(Integer id, EmployeeDTO dto) {
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));

        emp.setEmployeeName(dto.getEmployeeName());
        emp.setGender(Gender.fromDesc(dto.getGender()));
        emp.setPhone(dto.getPhone());
        emp.setEmail(dto.getEmail());
        emp.setPosition(dto.getPosition());

        // 更新部门
        if (dto.getOrgId() != null) {
            Department department = departmentRepository.findById(dto.getOrgId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            emp.setDepartment(department);
        } else {
            emp.setDepartment(null);
        }

        Employee saved = employeeRepository.save(emp);

        EmployeeDTO result = new EmployeeDTO();
        result.setEmployeeId(saved.getEmployeeId());
        result.setEmployeeName(saved.getEmployeeName());
        result.setGender(saved.getGender().getDesc());
        result.setPhone(saved.getPhone());
        result.setEmail(saved.getEmail());
        result.setPosition(saved.getPosition());
        if (saved.getDepartment() != null) {
            result.setOrgId(saved.getDepartment().getOrgId());
            result.setOrgName(saved.getDepartment().getOrgName());
        }

        return result;
    }

    /**
     * 部门调动
     */
    @Transactional
    public void transferDepartment(Integer employeeId, Integer newDepartmentId) {
        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        
        Department department = null;
        if (newDepartmentId != null) {
            department = departmentRepository.findById(newDepartmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found"));
        }
        
        emp.setDepartment(department);
        employeeRepository.save(emp);
    }

    /**
     * 删除员工（离职）
     */
    @Transactional
    public void deleteEmployee(Integer id) {
        employeeRepository.deleteById(id);
    }

    /**
     * 获取所有部门
     */
    public List<DepartmentDTO> getAllDepartments() {
        List<Department> departments = departmentRepository.findAll();
        return departments.stream()
                .map(dept -> new DepartmentDTO(dept.getOrgId(), dept.getOrgName()))
                .collect(Collectors.toList());
    }

    // ========== 十大重要事项管理 ==========

    /**
     * 获取所有重要事项
     */
    public List<ImportantMatterDTO> getAllImportantMatters() {
        List<ImportantMatter> matters = importantMatterRepository.findAll();
        return matters.stream()
                .map(matter -> {
                    ImportantMatterDTO dto = new ImportantMatterDTO();
                    dto.setMatterId(matter.getMatterId());
                    dto.setContent(matter.getContent());
                    dto.setDeadline(matter.getDeadline());
                    dto.setAssigneeId(matter.getAssignee().getEmployeeId());
                    dto.setAssigneeName(matter.getAssignee().getEmployeeName());
                    dto.setMatterStatus(matter.getMatterStatus());
                    dto.setMatterPriority(matter.getMatterPriority());
                    dto.setSerialNum(matter.getSerialNum());
                    dto.setVisibleRange(matter.getVisibleRange());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 创建重要事项
     */
    @Transactional
    public ImportantMatterDTO createImportantMatter(ImportantMatterDTO dto) {
        ImportantMatter matter = new ImportantMatter();
        matter.setContent(dto.getContent());
        matter.setDeadline(dto.getDeadline());
        matter.setMatterStatus(dto.getMatterStatus());
        matter.setMatterPriority(dto.getMatterPriority());
        matter.setSerialNum(dto.getSerialNum());
        matter.setVisibleRange(dto.getVisibleRange());

        Employee assignee = employeeRepository.findById(dto.getAssigneeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        matter.setAssignee(assignee);

        ImportantMatter saved = importantMatterRepository.save(matter);

        ImportantMatterDTO result = new ImportantMatterDTO();
        result.setMatterId(saved.getMatterId());
        result.setContent(saved.getContent());
        result.setDeadline(saved.getDeadline());
        result.setAssigneeId(saved.getAssignee().getEmployeeId());
        result.setAssigneeName(saved.getAssignee().getEmployeeName());
        result.setMatterStatus(saved.getMatterStatus());
        result.setMatterPriority(saved.getMatterPriority());
        result.setSerialNum(saved.getSerialNum());
        result.setVisibleRange(saved.getVisibleRange());

        return result;
    }

    /**
     * 更新重要事项
     */
    @Transactional
    public ImportantMatterDTO updateImportantMatter(Integer id, ImportantMatterDTO dto) {
        ImportantMatter matter = importantMatterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Important matter not found"));

        matter.setContent(dto.getContent());
        matter.setDeadline(dto.getDeadline());
        matter.setMatterStatus(dto.getMatterStatus());
        matter.setMatterPriority(dto.getMatterPriority());
        matter.setSerialNum(dto.getSerialNum());
        matter.setVisibleRange(dto.getVisibleRange());

        Employee assignee = employeeRepository.findById(dto.getAssigneeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        matter.setAssignee(assignee);

        ImportantMatter saved = importantMatterRepository.save(matter);

        ImportantMatterDTO result = new ImportantMatterDTO();
        result.setMatterId(saved.getMatterId());
        result.setContent(saved.getContent());
        result.setDeadline(saved.getDeadline());
        result.setAssigneeId(saved.getAssignee().getEmployeeId());
        result.setAssigneeName(saved.getAssignee().getEmployeeName());
        result.setMatterStatus(saved.getMatterStatus());
        result.setMatterPriority(saved.getMatterPriority());
        result.setSerialNum(saved.getSerialNum());
        result.setVisibleRange(saved.getVisibleRange());

        return result;
    }

    /**
     * 删除重要事项
     */
    @Transactional
    public void deleteImportantMatter(Integer id) {
        importantMatterRepository.deleteById(id);
    }

    // ========== 十大重要任务管理 ==========

    /**
     * 获取所有重要任务
     */
    public List<ImportantTaskDTO> getAllImportantTasks() {
        List<ImportantTask> tasks = importantTaskRepository.findAll();
        return tasks.stream()
                .map(task -> {
                    ImportantTaskDTO dto = new ImportantTaskDTO();
                    dto.setTaskId(task.getTaskId());
                    dto.setEmployeeId(task.getEmployee().getEmployeeId());
                    dto.setEmployeeName(task.getEmployee().getEmployeeName());
                    dto.setTaskContent(task.getTaskContent());
                    dto.setDeadline(task.getDeadline());
                    dto.setTaskStatus(task.getTaskStatus());
                    dto.setTaskPriority(task.getTaskPriority());
                    dto.setSerialNum(task.getSerialNum());
                    dto.setCreatedTime(task.getCreatedTime());
                    dto.setUpdatedTime(task.getUpdatedTime());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 创建重要任务
     */
    @Transactional
    public ImportantTaskDTO createImportantTask(ImportantTaskDTO dto) {
        ImportantTask task = new ImportantTask();
        task.setTaskContent(dto.getTaskContent());
        task.setDeadline(dto.getDeadline());
        task.setTaskStatus(dto.getTaskStatus() != null ? dto.getTaskStatus() : (byte) 0);
        task.setTaskPriority(dto.getTaskPriority() != null ? dto.getTaskPriority() : (byte) 1);
        task.setSerialNum(dto.getSerialNum());
        task.setCreatedTime(java.time.LocalDateTime.now());
        task.setUpdatedTime(java.time.LocalDateTime.now());

        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        task.setEmployee(employee);

        ImportantTask saved = importantTaskRepository.save(task);

        ImportantTaskDTO result = new ImportantTaskDTO();
        result.setTaskId(saved.getTaskId());
        result.setEmployeeId(saved.getEmployee().getEmployeeId());
        result.setEmployeeName(saved.getEmployee().getEmployeeName());
        result.setTaskContent(saved.getTaskContent());
        result.setDeadline(saved.getDeadline());
        result.setTaskStatus(saved.getTaskStatus());
        result.setTaskPriority(saved.getTaskPriority());
        result.setSerialNum(saved.getSerialNum());
        result.setCreatedTime(saved.getCreatedTime());
        result.setUpdatedTime(saved.getUpdatedTime());

        return result;
    }

    /**
     * 更新重要任务
     */
    @Transactional
    public ImportantTaskDTO updateImportantTask(Integer id, ImportantTaskDTO dto) {
        ImportantTask task = importantTaskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Important task not found"));

        task.setTaskContent(dto.getTaskContent());
        task.setDeadline(dto.getDeadline());
        task.setTaskStatus(dto.getTaskStatus());
        task.setTaskPriority(dto.getTaskPriority());
        task.setSerialNum(dto.getSerialNum());
        task.setUpdatedTime(java.time.LocalDateTime.now());

        Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        task.setEmployee(employee);

        ImportantTask saved = importantTaskRepository.save(task);

        ImportantTaskDTO result = new ImportantTaskDTO();
        result.setTaskId(saved.getTaskId());
        result.setEmployeeId(saved.getEmployee().getEmployeeId());
        result.setEmployeeName(saved.getEmployee().getEmployeeName());
        result.setTaskContent(saved.getTaskContent());
        result.setDeadline(saved.getDeadline());
        result.setTaskStatus(saved.getTaskStatus());
        result.setTaskPriority(saved.getTaskPriority());
        result.setSerialNum(saved.getSerialNum());
        result.setCreatedTime(saved.getCreatedTime());
        result.setUpdatedTime(saved.getUpdatedTime());

        return result;
    }

    /**
     * 删除重要任务
     */
    @Transactional
    public void deleteImportantTask(Integer id) {
        importantTaskRepository.deleteById(id);
    }

    // ========== 系统统计 ==========
    
    /**
     * 获取系统统计数据
     */
    public SystemStatsDTO getSystemStats() {
        SystemStatsDTO stats = new SystemStatsDTO();
        
        // 活跃用户数（员工总数）
        stats.setActiveUsers((int) employeeRepository.count());
        
        // 进行中任务数（taskStatus = 1）
        List<ImportantTask> allTasks = importantTaskRepository.findAll();
        long inProgressCount = allTasks.stream()
                .filter(task -> task.getTaskStatus() == 1)
                .count();
        stats.setInProgressTasks((int) inProgressCount);
        
        // 今日到期任务数（deadline 是今天的）
        long todayCount = allTasks.stream()
                .filter(task -> task.getDeadline() != null && 
                        task.getDeadline().toLocalDate().equals(java.time.LocalDate.now()))
                .count();
        stats.setTodayDeliveries((int) todayCount);
        
        // 任务完成率
        long totalTasks = allTasks.size();
        if (totalTasks > 0) {
            long completedTasks = allTasks.stream()
                    .filter(task -> task.getTaskStatus() == 2)
                    .count();
            stats.setCompletionRate((double) completedTasks / totalTasks * 100);
        } else {
            stats.setCompletionRate(0.0);
        }
        
        return stats;
    }
    
    /**
     * 获取最近系统活动（基于最近创建/更新的任务）
     */
    public List<ActivityLogDTO> getRecentActivities() {
        // 获取最近更新的任务（前5条）
        List<ImportantTask> recentTasks = importantTaskRepository.findAll().stream()
                .sorted((t1, t2) -> t2.getUpdatedTime().compareTo(t1.getUpdatedTime()))
                .limit(5)
                .collect(Collectors.toList());
        
        return recentTasks.stream()
                .map(task -> {
                    ActivityLogDTO activity = new ActivityLogDTO();
                    
                    // 判断任务状态
                    if (task.getTaskStatus() == 2) {
                        activity.setActivityType("COMPLETE_TASK");
                        activity.setDescription(task.getEmployee().getEmployeeName() + " 完成了任务 #" + task.getTaskId());
                    } else if (task.getTaskStatus() == 1) {
                        activity.setActivityType("UPDATE_TASK");
                        activity.setDescription(task.getEmployee().getEmployeeName() + " 更新了任务 #" + task.getTaskId());
                    } else {
                        activity.setActivityType("ADD_TASK");
                        activity.setDescription(task.getEmployee().getEmployeeName() + " 创建了新任务");
                    }
                    
                    activity.setEmployeeName(task.getEmployee().getEmployeeName());
                    activity.setTimestamp(task.getUpdatedTime());
                    activity.setRelativeTime(getRelativeTime(task.getUpdatedTime()));
                    
                    return activity;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 计算相对时间（如"10分钟前"）
     */
    private String getRelativeTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        
        java.time.Duration duration = java.time.Duration.between(dateTime, java.time.LocalDateTime.now());
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();
        
        if (minutes < 1) {
            return "刚刚";
        } else if (minutes < 60) {
            return minutes + "分钟前";
        } else if (hours < 24) {
            return hours + "小时前";
        } else {
            return days + "天前";
        }
    }
}


