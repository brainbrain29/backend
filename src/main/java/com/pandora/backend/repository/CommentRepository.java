package com.pandora.backend.repository;

import com.pandora.backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Integer> {
    List<Comment> findByNotice_NoticeIdAndParentIsNullOrderByCreatedAtDesc(Integer noticeId);
}
