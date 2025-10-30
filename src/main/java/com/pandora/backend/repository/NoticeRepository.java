package com.pandora.backend.repository;
import com.pandora.backend.entity.Notice;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Integer> {
    // 使用 JPQL 查询，按发布时间降序排列，取前10条
    @Query("SELECT n FROM Notice n ORDER BY n.createdTime DESC")
    List<Notice> findTop10Notices(Pageable pageable);
}