package com.pandora.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Chat response DTO for GLM API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDTO {
    
    /**
     * Response content.
     */
    private String content;
    
    /**
     * Model used for generation.
     */
    private String model;
    
    /**
     * Finish reason (stop, length, etc.).
     */
    private String finishReason;
    
    /**
     * Total tokens used.
     */
    private Integer totalTokens;
}
