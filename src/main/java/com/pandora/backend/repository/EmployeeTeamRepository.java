package com.pandora.backend.repository;

import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.EmployeeTeamId;
import com.pandora.backend.entity.Employee_Team;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 员工与团队关联的持久化接口。
 */
@Repository
public interface EmployeeTeamRepository extends JpaRepository<Employee_Team, EmployeeTeamId> {
    /**
     * 查询指定员工作为团队长所管理的团队编号。
     *
     * @param employeeId 员工标识
     * @param leaderFlag 团队长标记
     * @return 团队编号集合
     */
    @Query("SELECT et.team.teamId FROM Employee_Team et WHERE et.employee.employeeId = :employeeId AND et.isLeader = :leaderFlag")
    List<Integer> findLeaderTeamIds(@Param("employeeId") Integer employeeId, @Param("leaderFlag") Byte leaderFlag);

    /**
     * 查询团队中除指定员工外的其他成员。
     *
     * @param teamId            团队编号
     * @param excludeEmployeeId 被排除的员工编号
     * @return 团队成员集合
     */
    @Query("SELECT et.employee FROM Employee_Team et WHERE et.team.teamId = :teamId AND et.employee.employeeId <> :excludeEmployeeId")
    List<Employee> findTeammates(@Param("teamId") Integer teamId,
            @Param("excludeEmployeeId") Integer excludeEmployeeId);

    /**
     * 校验员工是否属于指定团队。
     *
     * @param teamId     团队编号
     * @param employeeId 员工编号
     * @return 若员工存在于团队内则返回 true
     */
    boolean existsByTeamTeamIdAndEmployeeEmployeeId(Integer teamId, Integer employeeId);

    /**
     * 查询所有团队长管理的团队信息
     * 
     * @param leaderFlag 团队长标记
     * @return Employee_Team 关系列表
     */
    @Query("SELECT et FROM Employee_Team et WHERE et.isLeader = :leaderFlag")
    List<Employee_Team> findAllTeamsWithLeaders(@Param("leaderFlag") Byte leaderFlag);

    /**
     * 根据部门查询该部门的所有团队(带团队长信息)
     * 
     * @param orgId      部门ID
     * @param leaderFlag 团队长标记
     * @return Employee_Team 关系列表
     */
    @Query("SELECT et FROM Employee_Team et WHERE et.team.department.orgId = :orgId AND et.isLeader = :leaderFlag")
    List<Employee_Team> findTeamsByDepartment(@Param("orgId") Integer orgId, @Param("leaderFlag") Byte leaderFlag);

    /**
     * 根据团队ID和职位查询团队成员
     * 直接在数据库层面过滤职位,提高性能
     * 
     * @param teamId    团队ID
     * @param positions 职位列表 (例如: 2=团队长, 3=员工)
     * @return 员工列表
     */
    @Query("SELECT et.employee FROM Employee_Team et WHERE et.team.teamId = :teamId AND et.employee.position IN :positions")
    List<Employee> findTeamMembersByPositions(@Param("teamId") Integer teamId,
            @Param("positions") List<Byte> positions);

    /**
     * 查询指定团队的团队长
     * 直接查询单个团队的团队长,避免查询所有团队
     * 
     * @param teamId     团队ID
     * @param leaderFlag 团队长标记 (1)
     * @return 团队长员工对象,如果不存在返回 null
     */
    @Query("SELECT et.employee FROM Employee_Team et WHERE et.team.teamId = :teamId AND et.isLeader = :leaderFlag")
    Employee findTeamLeader(@Param("teamId") Integer teamId, @Param("leaderFlag") Byte leaderFlag);
}
