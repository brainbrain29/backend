package com.pandora.backend.config;

import com.pandora.backend.service.TokenCleanupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@Component
public class StartupTokenCleaner {

    private int cleanupOnStart = 0;// 值为1时执行清理,不为1时则不清理

    @Autowired
    private TokenCleanupService tokenCleanupService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (cleanupOnStart == 1) {
            System.out.println("⚙️ 检测到 cleanup.on.start=1，启动时执行 token 清理...");
            tokenCleanupService.cleanExpiredTokens();
        } else {
            System.out.println("⚙️ cleanup.on.start != 1，跳过启动清理逻辑。");
        }
    }
}
