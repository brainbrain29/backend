package com.pandora.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * GLM API 聊天请求 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDTO {
    
    /**
     * 聊天消息列表
     */
    private List<ChatMessageDTO> messages;
    
    /**
     * 模型名称 (默认: glm-4)
     */
    private String model = "glm-4";
    
    /**
     * 是否使用流式模式
     */
    private Boolean stream = false;
    
    /**
     * 响应随机性温度参数 (0.0-1.0)
     */
    private Double temperature;
    
    /**
     * 生成的最大 token 数
     */
    private Integer maxTokens;
}
