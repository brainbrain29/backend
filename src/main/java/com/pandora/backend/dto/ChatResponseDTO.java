package com.pandora.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GLM API 聊天响应 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDTO {
    
    /**
     * 响应内容
     */
    private String content;
    
    /**
     * 用于生成的模型
     */
    private String model;
    
    /**
     * 结束原因 (stop, length 等)
     */
    private String finishReason;
    
    /**
     * 使用的总 token 数
     */
    private Integer totalTokens;
}
