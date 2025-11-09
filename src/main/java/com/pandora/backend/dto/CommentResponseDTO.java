package com.pandora.backend.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponseDTO {
    private Integer id;
    private String content;
    private LocalDateTime createdAt;

    // 只返回作者的核心信息
    private Integer authorId;
    private String authorName;
    // 所属事项信息
    private Integer noticeId;
    // 只返回父评论的ID
    private Integer parentId;
}