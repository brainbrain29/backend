package com.pandora.backend.repository;

import com.pandora.backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Integer> {

    /**
     * 根据事项ID查找所有顶级评论（父评论为null的），并按创建时间降序排列。
     * 修正了方法名，以匹配 Notice 实体中的 noticeId 属性。
     */
    List<Comment> findByNotice_NoticeIdAndParentIsNullOrderByCreatedAtDesc(Integer noticeId);

}