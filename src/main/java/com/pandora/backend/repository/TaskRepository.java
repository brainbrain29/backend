package com.pandora.backend.repository;

import com.pandora.backend.entity.Task;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {

    /**
     * 查询公司十大任务。
     * 这里的业务逻辑需要你来定义，因为 Task 实体中没有明确的“是否为公司任务”的标志。
     * 一个常见的做法是：比如 assignee 为 null 的任务，或者关联到某个特定“公司项目”的任务，就是公司任务。
     *
     * 【方案A - 假设任务负责人(assignee)为null的就是公司任务】
     * 这只是一个示例，你需要根据你的业务逻辑修改 WHERE 条件。
     */
    @Query("SELECT t FROM Task t WHERE t.assignee IS NULL ORDER BY t.endTime ASC")
    List<Task> findTop10CompanyTasks(Pageable pageable);

    /**
     * 查询分配给某个用户的十大个人任务。
     */
    @Query("SELECT t FROM Task t WHERE t.assignee.employeeId = :userId ORDER BY t.endTime ASC")
    List<Task> findTop10PersonalTasks(@Param("userId") Integer userId, Pageable pageable);


    List<Task> findByAssigneeEmployeeIdOrderByEndTimeAsc(Integer userId, Pageable pageable);

    // 我们在 LogService 中用到了这个，所以也需要加在这里
    List<Task> findByTaskId(Integer taskId);

}