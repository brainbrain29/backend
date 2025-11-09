package com.pandora.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import com.pandora.backend.entity.ImportantMatter;
import java.util.List;

@Repository
public interface ImportantMatterRepository extends JpaRepository<ImportantMatter, Integer> {

    /**
     * 查询公司十大事项
     * 按 ID 排序
     */
    @Query("SELECT im FROM ImportantMatter im ORDER BY im.matterId DESC")
    List<ImportantMatter> findTopMatters(Pageable pageable);
}
