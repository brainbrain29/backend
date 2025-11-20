package com.pandora.backend.repository;

import com.pandora.backend.entity.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Integer> {

    /**
     * 根据员工ID查询所有分析记录，按创建时间倒序
     */
    List<AiAnalysis> findByEmployeeEmployeeIdOrderByCreatedTimeDesc(Integer employeeId);

    /**
     * 查询员工最新的一条分析记录
     */
    @Query("SELECT a FROM AiAnalysis a WHERE a.employee.employeeId = :employeeId ORDER BY a.createdTime DESC LIMIT 1")
    Optional<AiAnalysis> findLatestByEmployeeId(@Param("employeeId") Integer employeeId);

    /**
     * 查询员工在指定时间范围内的分析记录
     */
    @Query("SELECT a FROM AiAnalysis a WHERE a.employee.employeeId = :employeeId " +
            "AND a.createdTime BETWEEN :startTime AND :endTime " +
            "ORDER BY a.createdTime DESC")
    List<AiAnalysis> findByEmployeeIdAndTimeRange(
            @Param("employeeId") Integer employeeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * 检查员工在指定时间后是否已有分析记录（用于判断是否需要重新生成）
     */
    @Query("SELECT COUNT(a) > 0 FROM AiAnalysis a WHERE a.employee.employeeId = :employeeId " +
            "AND a.createdTime > :afterTime")
    boolean existsByEmployeeIdAndCreatedTimeAfter(
            @Param("employeeId") Integer employeeId,
            @Param("afterTime") LocalDateTime afterTime);
}
