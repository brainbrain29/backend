package com.pandora.backend.repository;

import com.pandora.backend.entity.Project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Integer> {

    /**
     * 查询员工参与的所有项目
     * 通过 employee_team 表关联查询员工所在团队负责的项目
     * 使用 DISTINCT 避免重复结果
     * 
     * @param employeeId 员工ID
     * @return 员工参与的项目列表
     */
    @Query("SELECT DISTINCT p FROM Project p " +
            "INNER JOIN p.team t " +
            "INNER JOIN t.employeeTeams et " +
            "WHERE et.employee.employeeId = :employeeId")
    List<Project> findProjectsByEmployeeId(@Param("employeeId") Integer employeeId);

    /**
     * 查询员工创建的所有项目
     * 用于 CEO 和部门经理查询自己创建的项目
     * 
     * @param senderId 创建者ID
     * @return 创建的项目列表
     */
    @Query("SELECT p FROM Project p WHERE p.sender.employeeId = :senderId")
    List<Project> findBySenderEmployeeId(@Param("senderId") Integer senderId);
}