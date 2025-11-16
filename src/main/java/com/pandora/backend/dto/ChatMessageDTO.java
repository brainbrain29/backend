package com.pandora.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GLM API 聊天消息 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    
    /**
     * 消息发送者的角色 (user, assistant, system)
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
}
