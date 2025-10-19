package com.pandora.backend.repository;

import com.pandora.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByRefreshToken(String token);

    List<RefreshToken> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
