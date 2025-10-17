package com.pandora.backend.repository;

import com.pandora.backend.entity.Log;
import com.pandora.backend.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
