package com.pandora.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import com.pandora.backend.entity.ImportantTask;
import java.util.List;

@Repository
public interface ImportantTaskRepository extends JpaRepository<ImportantTask, Integer> {

    /**
     * 查询公司十大任务
     * 按序号排序
     */
    @Query("SELECT it FROM ImportantTask it ORDER BY it.serialNum ASC")
    List<ImportantTask> findTopTasks(Pageable pageable);
}
