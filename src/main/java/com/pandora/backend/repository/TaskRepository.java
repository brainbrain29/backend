package com.pandora.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
