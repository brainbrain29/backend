package com.pandora.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 搜索重要事项（支持部门、关键词过滤和分页）
     * 
     * 注意：ImportantMatter 实体类中没有 status 字段，已移除状态过滤
     * 
     * @param assigneeId 负责人ID（部门ID，null=全部）
     * @param keyword    搜索关键词（搜索内容，null=全部）
     * @param pageable   分页参数
     * @return 分页结果
     */
    @Query("""
            SELECT im FROM ImportantMatter im
            WHERE (:assigneeId IS NULL OR im.department.orgId = :assigneeId)
              AND (:keyword IS NULL OR LOWER(COALESCE(im.content, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY im.matterId DESC
            """)
    Page<ImportantMatter> searchMatters(
            @Param("assigneeId") Integer assigneeId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
