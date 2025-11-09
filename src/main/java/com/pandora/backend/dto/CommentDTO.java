// CommentDTO.java
package com.pandora.backend.dto;

import lombok.Data;

@Data
public class CommentDTO {
    private String content;
    private Integer parentId; // 回复哪条评论的ID，可以为 null
}