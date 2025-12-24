package com.pandora.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.pandora.backend.dto.*;
import com.pandora.backend.entity.*;
import com.pandora.backend.repository.*;
import com.pandora.backend.enums.Gender;
import com.pandora.backend.enums.Position;
import com.pandora.backend.security.EmployeeSecurityMapper;
import com.pandora.backend.security.PasswordHashService;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
    private TeamRepository teamRepository;

    @Autowired
    private EmployeeTeamRepository employeeTeamRepository;

    @Autowired
    private ImportantMatterRepository importantMatterRepository;

    @Autowired
    private ImportantTaskRepository importantTaskRepository;

    @Autowired
    private EmployeeSecurityMapper employeeSecurityMapper;

    @Autowired
    private PasswordHashService passwordHashService;

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
                    dto.setPhone(employeeSecurityMapper.getPhonePlain(emp));
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
     * 获取所有团队
     */
    @Transactional(readOnly = true)
    public List<TeamDTO> getAllTeams() {
        List<Team> teams = teamRepository.findAll();
        return teams.stream()
                .map(this::convertToTeamDto)
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
        dto.setPhone(employeeSecurityMapper.getPhonePlain(emp));
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
        if (dto == null) {
            throw new IllegalArgumentException("EmployeeDTO is null");
        }
        if (!StringUtils.hasText(dto.getEmployeeName())) {
            throw new IllegalArgumentException("员工姓名不能为空");
        }
        if (!StringUtils.hasText(dto.getGender())) {
            throw new IllegalArgumentException("性别不能为空");
        }
        if (!StringUtils.hasText(dto.getPhone())) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        if (!StringUtils.hasText(dto.getEmail())) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        if (dto.getPosition() == null) {
            throw new IllegalArgumentException("职位不能为空");
        }
        Employee emp = new Employee();
        emp.setEmployeeName(dto.getEmployeeName().trim());
        emp.setGender(Gender.fromDesc(dto.getGender()));
        employeeSecurityMapper.setPhone(emp, dto.getPhone().trim());
        emp.setEmail(dto.getEmail().trim());
        emp.setPosition(dto.getPosition());

        // 设置默认密码
        emp.setPassword(passwordHashService.hashPassword("123456"));

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
        result.setPhone(employeeSecurityMapper.getPhonePlain(saved));
        result.setEmail(saved.getEmail());
        result.setPosition(saved.getPosition());
        if (saved.getDepartment() != null) {
            result.setOrgId(saved.getDepartment().getOrgId());
            result.setOrgName(saved.getDepartment().getOrgName());
        }

        return result;
    }

    /**
     * 创建团队
     */
    @Transactional
    public TeamDTO createTeam(TeamDTO dto) {
        if (!StringUtils.hasText(dto.getTeamName())) {
            throw new IllegalArgumentException("团队名称不能为空");
        }
        if (dto.getOrgId() == null) {
            throw new IllegalArgumentException("请选择所属部门");
        }

        String trimmedName = dto.getTeamName().trim();
        if (teamRepository.existsByTeamNameAndDepartmentOrgId(trimmedName, dto.getOrgId())) {
            throw new IllegalArgumentException("该部门已存在同名团队");
        }

        Team team = new Team();
        team.setTeamName(trimmedName);
        Department department = departmentRepository.findById(dto.getOrgId())
                .orElseThrow(() -> new RuntimeException("Department not found"));
        team.setDepartment(department);

        Team saved = teamRepository.save(team);
        syncTeamMembers(saved, dto.getMemberIds(), dto.getLeaderId());
        return convertToTeamDto(saved);
    }

    /**
     * 更新员工信息
     */
    @Transactional
    public EmployeeDTO updateEmployee(Integer id, EmployeeDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("EmployeeDTO is null");
        }
        if (!StringUtils.hasText(dto.getEmployeeName())) {
            throw new IllegalArgumentException("员工姓名不能为空");
        }
        if (!StringUtils.hasText(dto.getGender())) {
            throw new IllegalArgumentException("性别不能为空");
        }
        if (!StringUtils.hasText(dto.getPhone())) {
            throw new IllegalArgumentException("手机号不能为空");
        }
        if (!StringUtils.hasText(dto.getEmail())) {
            throw new IllegalArgumentException("邮箱不能为空");
        }
        if (dto.getPosition() == null) {
            throw new IllegalArgumentException("职位不能为空");
        }
        Employee emp = employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + id));

        emp.setEmployeeName(dto.getEmployeeName().trim());
        emp.setGender(Gender.fromDesc(dto.getGender()));
        employeeSecurityMapper.setPhone(emp, dto.getPhone().trim());
        emp.setEmail(dto.getEmail().trim());
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
        result.setPhone(employeeSecurityMapper.getPhonePlain(saved));
        result.setEmail(saved.getEmail());
        result.setPosition(saved.getPosition());
        if (saved.getDepartment() != null) {
            result.setOrgId(saved.getDepartment().getOrgId());
            result.setOrgName(saved.getDepartment().getOrgName());
        }

        return result;
    }

    /**
     * 更新团队信息
     */
    @Transactional
    public TeamDTO updateTeam(Integer id, TeamDTO dto) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + id));

        if (!StringUtils.hasText(dto.getTeamName())) {
            throw new IllegalArgumentException("团队名称不能为空");
        }
        if (dto.getOrgId() == null) {
            throw new IllegalArgumentException("请选择所属部门");
        }

        String trimmedName = dto.getTeamName().trim();
        boolean departmentChanged = team.getDepartment() == null
                || !team.getDepartment().getOrgId().equals(dto.getOrgId());
        boolean nameChanged = !trimmedName.equals(team.getTeamName());
        if ((departmentChanged || nameChanged)
                && teamRepository.existsByTeamNameAndDepartmentOrgId(trimmedName, dto.getOrgId())) {
            throw new IllegalArgumentException("该部门已存在同名团队");
        }

        team.setTeamName(trimmedName);
        Department department = departmentRepository.findById(dto.getOrgId())
                .orElseThrow(() -> new RuntimeException("Department not found"));
        team.setDepartment(department);

        Team saved = teamRepository.save(team);
        syncTeamMembers(saved, dto.getMemberIds(), dto.getLeaderId());
        return convertToTeamDto(saved);
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
     * 删除团队
     */
    @Transactional
    public void deleteTeam(Integer id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + id));

        List<Employee_Team> relations = employeeTeamRepository.findByTeamTeamId(id);
        if (!relations.isEmpty()) {
            employeeTeamRepository.deleteAll(relations);
        }

        teamRepository.delete(team);
    }

    private TeamDTO convertToTeamDto(Team team) {
        TeamDTO dto = new TeamDTO();
        dto.setTeamId(team.getTeamId());
        dto.setTeamName(team.getTeamName());
        if (team.getDepartment() != null) {
            dto.setOrgId(team.getDepartment().getOrgId());
            dto.setOrgName(team.getDepartment().getOrgName());
        }

        List<Employee_Team> relations = employeeTeamRepository.findByTeamTeamId(team.getTeamId());
        dto.setMemberCount(relations.size());

        List<String> memberNames = new ArrayList<>();
        List<Integer> memberIds = new ArrayList<>();
        for (Employee_Team relation : relations) {
            Employee member = relation.getEmployee();
            if (member != null) {
                memberIds.add(member.getEmployeeId());
                memberNames.add(member.getEmployeeName());
                if (relation.getIsLeader() != null && relation.getIsLeader() == 1) {
                    dto.setLeaderId(member.getEmployeeId());
                    dto.setLeaderName(member.getEmployeeName());
                }
            }
        }

        dto.setMemberIds(memberIds);
        dto.setMemberNames(memberNames);

        return dto;
    }

    private void syncTeamMembers(Team team, List<Integer> memberIds, Integer leaderId) {
        List<Employee_Team> existing = employeeTeamRepository.findByTeamTeamId(team.getTeamId());
        if (!existing.isEmpty()) {
            employeeTeamRepository.deleteAll(existing);
        }

        if ((memberIds == null || memberIds.isEmpty()) && leaderId == null) {
            return;
        }

        Set<Integer> uniqueMemberIds = new LinkedHashSet<>();
        if (memberIds != null) {
            uniqueMemberIds.addAll(memberIds);
        }
        if (leaderId != null) {
            uniqueMemberIds.add(leaderId);
        }

        for (Integer memberId : uniqueMemberIds) {
            if (memberId == null) {
                continue;
            }
            Employee employee = employeeRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("Employee not found with id: " + memberId));

            Employee_Team relation = new Employee_Team();
            EmployeeTeamId relationId = new EmployeeTeamId();
            relationId.setEmployeeId(employee.getEmployeeId());
            relationId.setTeamId(team.getTeamId());
            relation.setId(relationId);
            relation.setEmployee(employee);
            relation.setTeam(team);
            relation.setIsLeader((byte) ((leaderId != null && leaderId.equals(memberId)) ? 1 : 0));

            employeeTeamRepository.save(relation);
        }
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
            employeeSecurityMapper.setPhone(emp, phone.trim());
        }

        if (StringUtils.hasText(newPassword)) {
            emp.setPassword(passwordHashService.hashPassword(newPassword.trim()));
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

    /**
     * 更新员工职位，并处理关联表的数据一致性。
     * 使用 @Transactional 注解确保所有数据库操作的原子性。
     *
     * @param employeeId 要更新的员工ID
     * @param dto        包含新职位和关联目标ID的数据
     * @return 更新后的员工信息DTO
     */
    @Transactional
    public EmployeeDTO updateEmployeePosition(Integer employeeId, UpdatePositionDTO dto) {
        // 1. 查找需要被更新的员工
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("未找到对应的员工，ID=" + employeeId));

        Position newPosition = dto.getNewPosition();
        if (newPosition == null) {
            throw new IllegalArgumentException("新职位不能为空。");
        }

        // 2. 清理该员工旧的职位职责，防止数据冲突
        cleanupOldPositionResponsibilities(employee);

        // 3. 根据新职位，执行不同的业务逻辑
        switch (newPosition) {
            case DEPARTMENT_MANAGER:
                if (dto.getTargetId() == null) {
                    throw new IllegalArgumentException("设置部门经理时必须指定部门ID。");
                }
                Department department = departmentRepository.findById(dto.getTargetId())
                        .orElseThrow(() -> new RuntimeException("未找到对应的部门，ID=" + dto.getTargetId()));

                // 可选：检查该部门是否已有经理，如果有，则先将其降级
                if (department.getManager() != null && !department.getManager().equals(employee)) {
                    Employee oldManager = department.getManager();
                    oldManager.setPosition(Position.EMPLOYEE.getCode()); // 降级为普通员工
                    employeeRepository.save(oldManager);
                }

                department.setManager(employee);
                departmentRepository.save(department);
                break;

            case TEAM_LEADER:
                if (dto.getTargetId() == null) {
                    throw new IllegalArgumentException("设置团队长时必须指定团队ID。");
                }
                Team team = teamRepository.findById(dto.getTargetId())
                        .orElseThrow(() -> new RuntimeException("未找到对应的团队，ID=" + dto.getTargetId()));

                // 将该员工设为新领导
                setTeamLeader(team, employee);
                break;

            case CEO:
            case EMPLOYEE:
                // CEO 和普通员工职位，目前没有关联表操作
                break;

            default:
                throw new UnsupportedOperationException(
                        "Position update for " + newPosition.name() + " is not supported yet.");
        }

        // 4. 最后，更新 employee 表中的 position 字段
        employee.setPosition(newPosition.getCode());
        Employee savedEmployee = employeeRepository.save(employee);

        // 5. 返回更新后的员工信息（复用您已有的转换逻辑）
        return convertToEmployeeDto(savedEmployee);
    }

    /**
     * 辅助方法：清理员工旧的职位职责，确保数据一致性。
     * 
     * @param employee 需要被清理的员工
     */
    private void cleanupOldPositionResponsibilities(Employee employee) {
        // 如果该员工之前是某个部门的经理，则将该部门的经理字段置空
        departmentRepository.findByManager(employee).ifPresent(dept -> {
            dept.setManager(null);
            departmentRepository.save(dept);
        });

        // 如果该员工之前是某个团队的队长，则将他的 is_leader 标志位移除
        employeeTeamRepository.findAll().forEach(relation -> {
            if (relation.getEmployee().equals(employee) && relation.getIsLeader() == 1) {
                relation.setIsLeader((byte) 0);
                employeeTeamRepository.save(relation);
            }
        });
    }

    /**
     * 辅助方法：设置团队的新领导，并自动处理旧领导的降职。
     *
     * @param team      目标团队
     * @param newLeader 新的领导者
     */
    private void setTeamLeader(Team team, Employee newLeader) {
        // 检查：新领导必须是该团队的成员
        Employee_Team newLeaderRelation = employeeTeamRepository.findByEmployeeAndTeam(newLeader, team)
                .orElseGet(() -> {
                    // 如果不是目标团队成员，查询其当前所属的所有团队，用于构造更友好的错误提示
                    List<Employee_Team> currentRelations = employeeTeamRepository.findByEmployee(newLeader);
                    String currentTeamsDesc;
                    if (currentRelations == null || currentRelations.isEmpty()) {
                        currentTeamsDesc = "当前未加入任何团队";
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (Employee_Team et : currentRelations) {
                            Team t = et.getTeam();
                            if (t != null && t.getTeamName() != null) {
                                if (sb.length() > 0) {
                                    sb.append("，");
                                }
                                sb.append(t.getTeamName());
                            }
                        }
                        currentTeamsDesc = sb.length() > 0 ? sb.toString() : "当前未加入任何团队";
                    }

                    throw new IllegalStateException(
                            "无法将该员工设置为团队长：该员工当前所属团队为：(" + currentTeamsDesc +
                                    ")，不包含目标团队（" + team.getTeamName() + "）。");
                });

        // 找到并降级现任领导（如果存在）
        employeeTeamRepository.findByTeamAndIsLeader(team, (byte) 1).ifPresent(oldLeaderRelation -> {
            if (!oldLeaderRelation.equals(newLeaderRelation)) {
                oldLeaderRelation.setIsLeader((byte) 0);
                employeeTeamRepository.save(oldLeaderRelation);
                // 同时更新旧领导在 employee 表中的职位
                Employee oldLeader = oldLeaderRelation.getEmployee();
                if (oldLeader.getPosition() == Position.TEAM_LEADER.getCode()) {
                    oldLeader.setPosition(Position.EMPLOYEE.getCode());
                    employeeRepository.save(oldLeader);
                }
            }
        });

        // 提升新领导
        newLeaderRelation.setIsLeader((byte) 1);
        employeeTeamRepository.save(newLeaderRelation);
    }

    /**
     * 辅助方法：将 Employee 实体转换为 EmployeeDTO。
     * (从您的 getAllEmployees 方法中提取的通用逻辑)
     *
     * @param emp 员工实体
     * @return 员工DTO
     */
    private EmployeeDTO convertToEmployeeDto(Employee emp) {
        EmployeeDTO dto = new EmployeeDTO();
        dto.setEmployeeId(emp.getEmployeeId());
        dto.setEmployeeName(emp.getEmployeeName());
        if (emp.getGender() != null) {
            dto.setGender(emp.getGender().getDesc());
        }
        dto.setPhone(employeeSecurityMapper.getPhonePlain(emp));
        dto.setEmail(emp.getEmail());
        dto.setPosition(emp.getPosition());
        if (emp.getDepartment() != null) {
            dto.setOrgId(emp.getDepartment().getOrgId());
            dto.setOrgName(emp.getDepartment().getOrgName());
        }
        return dto;
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
                    dto.setTitle(matter.getTitle());
                    dto.setContent(matter.getContent());
                    if (matter.getDepartment() != null) {
                        dto.setDepartmentId(matter.getDepartment().getOrgId());
                        dto.setDepartmentName(matter.getDepartment().getOrgName());
                    }
                    dto.setPublishTime(matter.getPublishTime());

                    // 添加默认值以兼容前端模板
                    dto.setDeadline(matter.getPublishTime()); // 使用发布时间作为截止日期
                    dto.setAssigneeName("系统"); // 默认负责人
                    dto.setAssigneeId(1); // 默认负责人ID
                    dto.setMatterStatus((byte) 0); // 默认状态：待处理
                    dto.setMatterPriority((byte) 1); // 默认优先级：中
                    dto.setSerialNum((byte) 1); // 默认序号
                    dto.setVisibleRange(0); // 默认可见范围

                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 搜索重要事项（优化版：使用数据库查询）
     * 
     * 注意：ImportantMatter 实体类中没有 status 字段，已移除状态过滤
     * 如需状态过滤，请在内存中进行
     * 
     * @param assigneeId 负责人ID（部门ID，null=全部）
     * @param keyword    搜索关键词（null=全部）
     * @param page       页码（从1开始）
     * @param size       每页大小
     * @return 包含分页信息的结果
     */
    public java.util.Map<String, Object> searchImportantMatters(
            Integer assigneeId, String keyword, int page, int size) {

        // 创建分页参数（Spring Data JPA 的页码从 0 开始）
        Pageable pageable = PageRequest.of(page - 1, size);

        // 调用 Repository 的搜索方法
        Page<ImportantMatter> pageResult = importantMatterRepository.searchMatters(
                assigneeId, keyword, pageable); // 转换为 DTO
        List<ImportantMatterDTO> matters = pageResult.getContent().stream()
                .map(matter -> {
                    ImportantMatterDTO dto = new ImportantMatterDTO();
                    dto.setMatterId(matter.getMatterId());
                    dto.setTitle(matter.getTitle());
                    dto.setContent(matter.getContent());
                    if (matter.getDepartment() != null) {
                        dto.setDepartmentId(matter.getDepartment().getOrgId());
                        dto.setDepartmentName(matter.getDepartment().getOrgName());
                    }
                    dto.setPublishTime(matter.getPublishTime());

                    // 添加默认值以兼容前端模板
                    dto.setDeadline(matter.getPublishTime());
                    dto.setAssigneeName("系统");
                    dto.setAssigneeId(1);
                    dto.setMatterStatus((byte) 0);
                    dto.setMatterPriority((byte) 1);
                    dto.setSerialNum((byte) 1);
                    dto.setVisibleRange(0);

                    return dto;
                })
                .collect(Collectors.toList());

        // 构建返回结果
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("matters", matters);
        result.put("totalCount", pageResult.getTotalElements());
        result.put("currentPage", page);
        result.put("totalPages", pageResult.getTotalPages());
        result.put("pageSize", size);

        return result;
    }

    /**
     * 获取单个重要事项
     */
    public ImportantMatterDTO getImportantMatterById(Integer id) {
        ImportantMatter matter = importantMatterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Important matter not found"));

        ImportantMatterDTO result = new ImportantMatterDTO();
        result.setMatterId(matter.getMatterId());
        result.setTitle(matter.getTitle());
        result.setContent(matter.getContent());
        if (matter.getDepartment() != null) {
            result.setDepartmentId(matter.getDepartment().getOrgId());
            result.setDepartmentName(matter.getDepartment().getOrgName());
        }
        result.setPublishTime(matter.getPublishTime());

        return result;
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
        } else {
            matter.setDepartment(null);
        }

        ImportantMatter saved = importantMatterRepository.save(matter);

        ImportantMatterDTO result = new ImportantMatterDTO();
        result.setMatterId(saved.getMatterId());
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
        } else {
            matter.setDepartment(null);
        }

        ImportantMatter saved = importantMatterRepository.save(matter);

        ImportantMatterDTO result = new ImportantMatterDTO();
        result.setMatterId(saved.getMatterId());
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
