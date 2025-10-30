package com.pandora.backend.repository;
import com.pandora.backend.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.domain.Pageable;
import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Integer> {
    @Query("SELECT n FROM Notice n ORDER BY n.createdTime DESC")
    List<Notice> findTop10Notices(Pageable pageable);}