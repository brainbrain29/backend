package com.pandora.backend.repository;

import org.springframework.data.domain.Pageable;
import java.util.List;
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
    @Query("SELECT t FROM Task t WHERE t.assignee IS NULL ORDER BY t.endTime ASC")
    List<Task> findTop10CompanyTasks(Pageable pageable);
    @Query("SELECT t FROM Task t WHERE t.assignee.employeeId = :userId ORDER BY t.endTime ASC")
    List<Task> findTop10PersonalTasks(@Param("userId") Integer userId, Pageable pageable);

    /**
     * Spring Data JPA 也可以根据方法名自动生成查询，这更简洁。
     * 下面这个方法和上面的 @Query 功能完全一样。
     * 你可以选择使用 @Query 或者下面的方法命名规则。
     */
    List<Task> findByAssigneeEmployeeIdOrderByEndTimeAsc(Integer userId, Pageable pageable);

    // 我们在 LogService 中用到了这个，所以也需要加在这里
    List<Task> findByTaskId(Integer taskId);
}
