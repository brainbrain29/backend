package com.pandora.backend.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pandora.backend.entity.Task;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {
    // 根据发送者ID查询任务
    List<Task> findBySenderEmployeeId(Integer senderId);

    // 根据执行者ID查询任务
    List<Task> findByAssigneeEmployeeId(Integer assigneeId);

    // 根据里程碑ID查询任务
    List<Task> findByMilestoneMilestoneId(Integer milestoneId);

    // 根据任务状态查询
    List<Task> findByTaskStatus(Byte taskStatus);

    // 我们在 LogService 中用到了这个，所以也需要加在这里
    List<Task> findByTaskId(Integer taskId);

    @Query("""
            SELECT DISTINCT t FROM Task t
            LEFT JOIN t.assignee assignee
            LEFT JOIN t.sender sender
            WHERE LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(t.content, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(assignee.employeeName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(COALESCE(sender.employeeName, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    List<Task> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT t FROM Task t WHERE t.assignee IS NULL ORDER BY t.endTime ASC")
    List<Task> findTop10CompanyTasks(Pageable pageable);

    @Query("SELECT t FROM Task t WHERE t.assignee.employeeId = :userId ORDER BY t.endTime ASC")
    List<Task> findTop10PersonalTasks(@Param("userId") Integer userId, Pageable pageable);

  
}
