package com.pandora.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatStreamRequestDTO {
    private String sessionId;
    private String message;
    private Integer weeks;
    private Boolean includeAttachments;
}
