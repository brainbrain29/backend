package com.pandora.backend.service;

import com.pandora.backend.dto.CommentDTO;
import com.pandora.backend.dto.CommentResponseDTO; // 导入新的响应 DTO
import com.pandora.backend.entity.Comment;
import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.Notice;
import com.pandora.backend.repository.CommentRepository;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.repository.NoticeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommentService {

    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private NoticeRepository noticeRepository;
    @Autowired
    private EmployeeRepository employeeRepository;

    /**
     * 根据事项ID获取评论列表 (返回 DTO 列表)
     */
    public List<CommentResponseDTO> getCommentsByNoticeId(Integer noticeId) {
        List<Comment> comments = commentRepository.findByNotice_NoticeIdAndParentIsNullOrderByCreatedAtDesc(noticeId);
        // 将实体列表转换为 DTO 列表
        return comments.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 创建一条新评论 (返回 DTO)
     */
    public CommentResponseDTO createComment(CommentDTO commentDTO, Integer noticeId, Integer authorId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new RuntimeException("Notice not found with id: " + noticeId));
        Employee author = employeeRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Employee not found with id: " + authorId));

        Comment newComment = new Comment();
        newComment.setContent(commentDTO.getContent());
        newComment.setNotice(notice);
        newComment.setAuthor(author);

        if (commentDTO.getParentId() != null) {
            Comment parentComment = commentRepository.findById(commentDTO.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found with id: " + commentDTO.getParentId()));
            newComment.setParent(parentComment);
        }

        Comment savedComment = commentRepository.save(newComment);
        // 将保存后的实体转换为 DTO 再返回
        return convertToResponseDto(savedComment);
    }

    // 你未来可能会添加的其他方法，比如删除...
    public void deleteComment(Integer commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new RuntimeException("Comment not found with id: " + commentId);
        }
        commentRepository.deleteById(commentId);
    }


    // ==========================================
    // ==== 私有的、用于转换的核心辅助方法 ====
    // ==========================================
    private CommentResponseDTO convertToResponseDto(Comment comment) {
        CommentResponseDTO dto = new CommentResponseDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setCreatedAt(comment.getCreatedAt());

        if (comment.getAuthor() != null) {
            dto.setAuthorId(comment.getAuthor().getEmployeeId());
            dto.setAuthorName(comment.getAuthor().getEmployeeName());
        }

        if (comment.getNotice() != null) {
            dto.setNoticeId(comment.getNotice().getNoticeId());
        }

        if (comment.getParent() != null) {
            dto.setParentId(comment.getParent().getId());
        }

        return dto;
    }
}