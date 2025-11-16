package com.pandora.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * GLM-4.6 API 配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "glm")
public class GlmConfig {
    
    /**
     * GLM API 密钥
     */
    private String apiKey;
    
    /**
     * GLM API 基础 URL
     */
    private String apiUrl = "https://open.bigmodel.cn/api/paas/v4";
    
    /**
     * 默认模型名称
     */
    private String model = "glm-4";
    
    /**
     * 请求超时时间(秒)
     */
    private Integer timeout = 60;
}
