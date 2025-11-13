package com.pandora.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Chat request DTO for GLM API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDTO {
    
    /**
     * List of chat messages.
     */
    private List<ChatMessageDTO> messages;
    
    /**
     * Model name (default: glm-4).
     */
    private String model = "glm-4";
    
    /**
     * Whether to use streaming mode.
     */
    private Boolean stream = false;
    
    /**
     * Temperature for response randomness (0.0-1.0).
     */
    private Double temperature;
    
    /**
     * Maximum tokens to generate.
     */
    private Integer maxTokens;
}
