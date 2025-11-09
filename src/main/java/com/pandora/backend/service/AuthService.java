package com.pandora.backend.service;

import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.RefreshToken;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.repository.RefreshTokenRepository;
import com.pandora.backend.util.JwtUtil;
import com.pandora.backend.dto.TokenPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthService.class);
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_USED = "USED";
    private static final String ERROR_MESSAGE_INTERNAL_SERVER = "Internal Server Error";

    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    @Autowired
    private JwtUtil jwtUtil;

    public String login(String phone, String password) {
        Employee emp = employeeRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!emp.getPassword().equals(password)) {
            throw new RuntimeException("Invalid password");
        }

        return jwtUtil.generateAccessToken(emp.getEmployeeId(), emp.getPosition());
    }

    public TokenPair generateTokens(Employee emp) {
        String access = jwtUtil.generateAccessToken(emp.getEmployeeId(), emp.getPosition());
        String refresh = jwtUtil.generateRefreshToken(emp.getEmployeeId());

        RefreshToken entity = new RefreshToken();
        entity.setUserId(emp.getEmployeeId());
        entity.setRefreshToken(refresh);
        entity.setStatus(STATUS_ACTIVE);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(entity);

        return new TokenPair(access, refresh);
    }

    public TokenPair refreshToken(String oldRefreshToken) {
        RefreshToken token = refreshTokenRepository.findByRefreshToken(oldRefreshToken)
                .orElseThrow(() -> handleRefreshTokenError(oldRefreshToken, "Refresh token not found"));

        validateRefreshToken(token);

        token.setStatus(STATUS_USED);
        refreshTokenRepository.save(token);

        Integer userId = jwtUtil.extractUserId(oldRefreshToken);
        Employee emp = employeeRepository.findById(userId)
                .orElseThrow(() -> handleRefreshTokenError(oldRefreshToken, "User not found for refresh token"));

        return generateTokens(emp);
    }

    private void validateRefreshToken(RefreshToken token) {
        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw handleRefreshTokenError(token.getRefreshToken(), "Refresh token expired");
        }
        if (!STATUS_ACTIVE.equals(token.getStatus())) {
            throw handleRefreshTokenError(token.getRefreshToken(), "Refresh token status is " + token.getStatus());
        }
    }

    private RuntimeException handleRefreshTokenError(String refreshToken, String reason) {
        LOGGER.error("Failed to refresh token: reason={}, refreshToken={}", reason, refreshToken);
        return new RuntimeException(ERROR_MESSAGE_INTERNAL_SERVER);
    }
}
