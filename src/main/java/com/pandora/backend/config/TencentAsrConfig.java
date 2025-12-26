package com.pandora.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "tencent.asr")
public class TencentAsrConfig {

    private String appId;

    private String secretId;

    private String secretKey;

    private String engineModelType = "16k_zh";

    private Integer voiceFormat = 1;

    private Integer needVad = 1;

    private Integer expiresInSeconds = 600;
}
