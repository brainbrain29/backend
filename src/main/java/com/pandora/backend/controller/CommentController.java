package com.pandora.backend.controller;

import com.pandora.backend.dto.CommentDTO;
import com.pandora.backend.dto.CommentResponseDTO; // 导入新的响应 DTO
import com.pandora.backend.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 获取某个事项下的评论列表
     */
    @GetMapping("/notices/{noticeId}/comments")
    public ResponseEntity<List<CommentResponseDTO>> getCommentsForNotice(@PathVariable Integer noticeId) {
        List<CommentResponseDTO> comments = commentService.getCommentsByNoticeId(noticeId);
        return ResponseEntity.ok(comments);
    }

    /**
     * 在某个事项下发表新评论
     */
    @PostMapping("/notices/{noticeId}/comments")
    public ResponseEntity<CommentResponseDTO> addCommentToNotice(@PathVariable Integer noticeId, @RequestBody CommentDTO commentDTO) {
        // TODO: 替换为从 Spring Security 获取当前登录用户
        Integer currentUserId = 1;

        CommentResponseDTO newComment = commentService.createComment(commentDTO, noticeId, currentUserId);
        return new ResponseEntity<>(newComment, HttpStatus.CREATED);
    }

    /**
     * 删除一条评论 (新增)
     */
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Integer commentId) {
        // TODO: 在 Service 层增加权限校验逻辑，确保只有作者或管理员能删除
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }
}