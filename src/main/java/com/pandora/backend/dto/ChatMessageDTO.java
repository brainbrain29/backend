package com.pandora.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat message DTO for GLM API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDTO {
    
    /**
     * Role of the message sender (user, assistant, system).
     */
    private String role;
    
    /**
     * Content of the message.
     */
    private String content;
}
