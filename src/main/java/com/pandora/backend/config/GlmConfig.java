package com.pandora.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for GLM-4.6 API.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "glm")
public class GlmConfig {
    
    /**
     * GLM API key.
     */
    private String apiKey;
    
    /**
     * GLM API base URL.
     */
    private String apiUrl = "https://open.bigmodel.cn/api/paas/v4";
    
    /**
     * Default model name.
     */
    private String model = "glm-4";
    
    /**
     * Request timeout in seconds.
     */
    private Integer timeout = 60;
}
