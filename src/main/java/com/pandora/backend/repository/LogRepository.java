package com.pandora.backend.repository;

import com.pandora.backend.entity.Log;
import com.pandora.backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface LogRepository extends JpaRepository<Log, Integer> {
    // 使用 JOIN FETCH 优化 findByTask_TaskId
    @Query("SELECT l FROM Log l LEFT JOIN FETCH l.employee LEFT JOIN FETCH l.task WHERE l.task.taskId = :taskId")
    List<Log> findByTask_TaskId(@Param("taskId") Integer taskId);

    // 使用 JOIN FETCH 优化 findByEmployeeEmployeeIdAndCreatedTimeBetween
    @Query("SELECT l FROM Log l LEFT JOIN FETCH l.employee LEFT JOIN FETCH l.task WHERE l.employee.employeeId = :userId AND l.createdTime BETWEEN :start AND :end")
    List<Log> findByEmployeeEmployeeIdAndCreatedTimeBetween(@Param("userId") Integer userId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 使用 JOIN FETCH 优化 findAll
    @Query("SELECT l FROM Log l LEFT JOIN FETCH l.employee LEFT JOIN FETCH l.task")
    List<Log> findAllWithDetails();

    // 搜索方法也需要优化
    @Query("SELECT l FROM Log l LEFT JOIN FETCH l.employee LEFT JOIN FETCH l.task WHERE l.content LIKE %:keyword%")
    List<Log> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT l FROM Log l LEFT JOIN FETCH l.employee LEFT JOIN FETCH l.task WHERE l.logId = :logId")
    Optional<Log> findByIdWithDetails(@Param("logId") Integer logId);


}
