package com.pandora.backend.repository;

import com.pandora.backend.entity.Log;
import com.pandora.backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogRepository extends JpaRepository<Log, Integer> {
    // 有task类时查询
    List<Log> findByTask(Task task);

    // 直接使用task_id
    List<Log> findByTask_TaskId(Integer taskId);

    List<Log> findByCreatedTimeBetween(LocalDateTime start, LocalDateTime end);
    @Query("SELECT l FROM Log l WHERE l.employee.employeeId = :userId AND l.createdTime >= :startOfDay AND l.createdTime < :endOfDay ORDER BY l.createdTime DESC")
    List<Log> findTodayLogsByEmployeeId(@Param("userId") Integer userId, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("""
            SELECT DISTINCT l FROM Log l
            LEFT JOIN l.employee employee
            LEFT JOIN l.task task
            WHERE LOWER(l.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(l.employeeLocation, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(l.attachment, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(employee.employeeName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(task.title, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    List<Log> searchByKeyword(@Param("keyword") String keyword);

}
