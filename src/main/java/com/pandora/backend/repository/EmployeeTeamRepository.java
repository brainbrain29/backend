package com.pandora.backend.repository;

import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.EmployeeTeamId;
import com.pandora.backend.entity.Employee_Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeTeamRepository extends JpaRepository<Employee_Team, EmployeeTeamId> {
    List<Employee_Team> findByTeamTeamId(Integer teamId);
    long countByTeamTeamId(Integer teamId);

    /**
     * 查询指定团队中符合职位要求的员工
     * @param teamId 团队ID
     * @param positions 职位列表
     * @return 员工列表
     */
    @Query("SELECT et.employee FROM Employee_Team et " +
           "WHERE et.team.teamId = :teamId " +
           "AND et.employee.position IN :positions")
    List<Employee> findTeamMembersByPositions(@Param("teamId") Integer teamId, 
                                               @Param("positions") List<Byte> positions);

    /**
     * 查询所有团队的团队长关系
     * @param isLeader 是否为团队长 (1=是)
     * @return 团队长关系列表
     */
    @Query("SELECT et FROM Employee_Team et WHERE et.isLeader = :isLeader")
    List<Employee_Team> findAllTeamsWithLeaders(@Param("isLeader") Byte isLeader);

    /**
     * 查询指定部门的团队长关系
     * @param orgId 部门ID
     * @param isLeader 是否为团队长 (1=是)
     * @return 团队长关系列表
     */
    @Query("SELECT et FROM Employee_Team et " +
           "WHERE et.team.department.orgId = :orgId " +
           "AND et.isLeader = :isLeader")
    List<Employee_Team> findTeamsByDepartment(@Param("orgId") Integer orgId, 
                                               @Param("isLeader") Byte isLeader);

    /**
     * 查询团队长负责的团队ID列表
     * @param leaderId 团队长ID
     * @param isLeader 是否为团队长 (1=是)
     * @return 团队ID列表
     */
    @Query("SELECT et.team.teamId FROM Employee_Team et " +
           "WHERE et.employee.employeeId = :leaderId " +
           "AND et.isLeader = :isLeader")
    List<Integer> findLeaderTeamIds(@Param("leaderId") Integer leaderId, 
                                     @Param("isLeader") Byte isLeader);

    /**
     * 查询团队成员(排除指定员工,通常是团队长)
     * @param teamId 团队ID
     * @param excludeEmployeeId 要排除的员工ID
     * @return 员工列表
     */
    @Query("SELECT et.employee FROM Employee_Team et " +
           "WHERE et.team.teamId = :teamId " +
           "AND et.employee.employeeId != :excludeEmployeeId")
    List<Employee> findTeammates(@Param("teamId") Integer teamId, 
                                  @Param("excludeEmployeeId") Integer excludeEmployeeId);

    /**
     * 检查员工是否在指定团队中
     * @param teamId 团队ID
     * @param employeeId 员工ID
     * @return 是否存在
     */
    boolean existsByTeamTeamIdAndEmployeeEmployeeId(Integer teamId, Integer employeeId);

    /**
     * 查询团队的团队长
     * @param teamId 团队ID
     * @param isLeader 是否为团队长 (1=是)
     * @return 团队长员工对象,如果不存在则返回null
     */
    @Query("SELECT et.employee FROM Employee_Team et " +
           "WHERE et.team.teamId = :teamId " +
           "AND et.isLeader = :isLeader")
    Employee findTeamLeader(@Param("teamId") Integer teamId, 
                            @Param("isLeader") Byte isLeader);
}