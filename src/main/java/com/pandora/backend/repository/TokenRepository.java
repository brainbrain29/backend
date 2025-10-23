package com.pandora.backend.repository;

import com.pandora.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

public interface TokenRepository extends JpaRepository<RefreshToken, Long> {

    @Transactional
    int deleteByExpiresAtBefore(LocalDateTime now);
}
