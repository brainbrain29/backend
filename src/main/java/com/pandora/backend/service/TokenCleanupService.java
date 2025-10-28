package com.pandora.backend.service;

import com.pandora.backend.repository.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupService.class);

    @Autowired
    private TokenRepository tokenRepository;

    public void cleanExpiredTokens() {
        int deleted = tokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("🧹 TokenCleanup: 清理过期 token {} 条", deleted);
    }
}