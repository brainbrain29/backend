package com.pandora.backend.config;

import com.pandora.backend.service.TokenCleanupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

/**
 * 启动时清理过期 Token
 * 配置项：cleanup.on.start（0=不清理，1=清理）
 */
@Component
public class StartupTokenCleaner {

    @Value("${cleanup.on.start:0}")  // 从配置文件读取，默认 0
    private int cleanupOnStart;

    @Autowired
    private TokenCleanupService tokenCleanupService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (cleanupOnStart == 1) {
            System.out.println("⚙️ 检测到 cleanup.on.start=1，启动时执行 token 清理...");
            tokenCleanupService.cleanExpiredTokens();
        } else {
            System.out.println("⚙️ cleanup.on.start=" + cleanupOnStart + "，跳过启动清理逻辑。");
        }
    }
}
