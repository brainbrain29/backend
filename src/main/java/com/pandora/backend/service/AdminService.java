package com.pandora.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.pandora.backend.dto.*;
import com.pandora.backend.entity.*;
import com.pandora.backend.repository.*;
import com.pandora.backend.enums.Gender;

import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

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
     * 根据 ID 获取管理员信息
     */
    public EmployeeDTO getAdminById(Integer adminId) {
        Employee emp = employeeRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));

        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmployeeId(emp.getEmployeeId());
        dto.setEmployeeName(emp.getEmployeeName());
        dto.setGender(emp.getGender() != null ? emp.getGender().getDesc() : null);
        dto.setPhone(emp.getPhone());
        dto.setEmail(emp.getEmail());
        dto.setPosition(emp.getPosition());
        if (emp.getDepartment() != null) {
            dto.setOrgId(emp.getDepartment().getOrgId());
            dto.setOrgName(emp.getDepartment().getOrgName());
        }
        return dto;
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
     * 更新管理员个人资料
     */
    @Transactional
    public Employee updateAdminProfile(Integer adminId, String name, String email, String phone,
            String newPassword, MultipartFile avatarFile) {
        Employee emp = employeeRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("管理员不存在"));

        if (!StringUtils.hasText(name)) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        if (!StringUtils.hasText(email)) {
            throw new IllegalArgumentException("邮箱不能为空");
        }

        emp.setEmployeeName(name.trim());
        emp.setEmail(email.trim());
        if (StringUtils.hasText(phone)) {
            emp.setPhone(phone.trim());
        }

        if (StringUtils.hasText(newPassword)) {
            emp.setPassword(newPassword.trim());
        }

        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
                Files.createDirectories(uploadPath);

                String extension = StringUtils.getFilenameExtension(avatarFile.getOriginalFilename());
                String extPart = StringUtils.hasText(extension) ? ("." + extension) : "";
                String filename = "avatar-" + adminId + "-" + System.currentTimeMillis() + extPart;
                Path targetPath = uploadPath.resolve(filename);
                Files.copy(avatarFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                emp.setAvatarUrl("/uploads/" + filename);
            } catch (IOException ex) {
                throw new RuntimeException("头像上传失败", ex);
            }
        }

        return employeeRepository.save(emp);
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
                    dto.setEventId(matter.getMatterId());
                    dto.setTitle(matter.getTitle());
                    dto.setContent(matter.getContent());
                    if (matter.getDepartment() != null) {
                        dto.setDepartmentName(matter.getDepartment().getOrgName());
                    }
                    dto.setPublishTime(matter.getPublishTime());
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
        matter.setTitle(dto.getTitle());
        matter.setContent(dto.getContent());
        matter.setPublishTime(java.time.LocalDateTime.now());

        // 设置部门
        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            matter.setDepartment(department);
        }

        ImportantMatter saved = importantMatterRepository.save(matter);

        ImportantMatterDTO result = new ImportantMatterDTO();
        result.setEventId(saved.getMatterId());
        result.setTitle(saved.getTitle());
        result.setContent(saved.getContent());
        if (saved.getDepartment() != null) {
            result.setDepartmentId(saved.getDepartment().getOrgId());
            result.setDepartmentName(saved.getDepartment().getOrgName());
        }
        result.setPublishTime(saved.getPublishTime());

        return result;
    }

    /**
     * 更新重要事项
     */
    @Transactional
    public ImportantMatterDTO updateImportantMatter(Integer id, ImportantMatterDTO dto) {
        ImportantMatter matter = importantMatterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Important matter not found"));

        matter.setTitle(dto.getTitle());
        matter.setContent(dto.getContent());

        // 更新部门
        if (dto.getDepartmentId() != null) {
            Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            matter.setDepartment(department);
        }

        ImportantMatter saved = importantMatterRepository.save(matter);

        ImportantMatterDTO result = new ImportantMatterDTO();
        result.setEventId(saved.getMatterId());
        result.setTitle(saved.getTitle());
        result.setContent(saved.getContent());
        if (saved.getDepartment() != null) {
            result.setDepartmentId(saved.getDepartment().getOrgId());
            result.setDepartmentName(saved.getDepartment().getOrgName());
        }
        result.setPublishTime(saved.getPublishTime());

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

                    // 将 code 转换为中文描述
                    if (task.getTaskStatus() != null) {
                        String statusDesc = switch (task.getTaskStatus()) {
                            case 0 -> "待处理";
                            case 1 -> "进行中";
                            case 2 -> "已完成";
                            default -> "未知";
                        };
                        dto.setTaskStatus(statusDesc);
                    }
                    if (task.getTaskPriority() != null) {
                        String priorityDesc = switch (task.getTaskPriority()) {
                            case 0 -> "低";
                            case 1 -> "中";
                            case 2 -> "高";
                            default -> "未知";
                        };
                        dto.setTaskPriority(priorityDesc);
                    }

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

        // 将中文描述转换为 code
        if (dto.getTaskStatus() != null) {
            byte statusCode = switch (dto.getTaskStatus()) {
                case "待处理" -> 0;
                case "进行中" -> 1;
                case "已完成" -> 2;
                default -> 0;
            };
            task.setTaskStatus(statusCode);
        } else {
            task.setTaskStatus((byte) 0);
        }

        if (dto.getTaskPriority() != null) {
            byte priorityCode = switch (dto.getTaskPriority()) {
                case "低" -> 0;
                case "中" -> 1;
                case "高" -> 2;
                default -> 1;
            };
            task.setTaskPriority(priorityCode);
        } else {
            task.setTaskPriority((byte) 1);
        }

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

        // 将 code 转换为中文描述
        result.setTaskStatus(switch (saved.getTaskStatus()) {
            case 0 -> "待处理";
            case 1 -> "进行中";
            case 2 -> "已完成";
            default -> "未知";
        });
        result.setTaskPriority(switch (saved.getTaskPriority()) {
            case 0 -> "低";
            case 1 -> "中";
            case 2 -> "高";
            default -> "未知";
        });

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

        // 将中文描述转换为 code
        if (dto.getTaskStatus() != null) {
            byte statusCode = switch (dto.getTaskStatus()) {
                case "待处理" -> 0;
                case "进行中" -> 1;
                case "已完成" -> 2;
                default -> 0;
            };
            task.setTaskStatus(statusCode);
        }

        if (dto.getTaskPriority() != null) {
            byte priorityCode = switch (dto.getTaskPriority()) {
                case "低" -> 0;
                case "中" -> 1;
                case "高" -> 2;
                default -> 1;
            };
            task.setTaskPriority(priorityCode);
        }

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

        // 将 code 转换为中文描述
        result.setTaskStatus(switch (saved.getTaskStatus()) {
            case 0 -> "待处理";
            case 1 -> "进行中";
            case 2 -> "已完成";
            default -> "未知";
        });
        result.setTaskPriority(switch (saved.getTaskPriority()) {
            case 0 -> "低";
            case 1 -> "中";
            case 2 -> "高";
            default -> "未知";
        });

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
