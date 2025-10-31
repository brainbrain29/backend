package com.pandora.backend.service;

import com.pandora.backend.entity.Employee;
import com.pandora.backend.entity.RefreshToken;
import com.pandora.backend.repository.EmployeeRepository;
import com.pandora.backend.repository.RefreshTokenRepository;
import com.pandora.backend.util.JwtUtil;
import com.pandora.backend.dto.TokenPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

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
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setExpiresAt(LocalDateTime.now().plusDays(7));
        refreshTokenRepository.save(entity);

        // 只保留最近2个 Token
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdOrderByCreatedAtDesc(emp.getEmployeeId());
        if (tokens.size() > 2) {
            // 删除第 3 个及之后的所有旧 Token
            List<RefreshToken> tokensToDelete = tokens.subList(2, tokens.size());
            refreshTokenRepository.deleteAll(tokensToDelete);
            System.out.println("🗑️ 清理用户 " + emp.getEmployeeId() + " 的 " + tokensToDelete.size() + " 个旧 Token");
        }

        return new TokenPair(access, refresh);
    }

    public TokenPair refreshToken(String oldRefreshToken) {
        RefreshToken token = refreshTokenRepository.findByRefreshToken(oldRefreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (token.getExpiresAt().isBefore(LocalDateTime.now()) || !"ACTIVE".equals(token.getStatus())) {
            throw new RuntimeException("Refresh token expired or revoked");
        }

        token.setStatus("USED");
        refreshTokenRepository.save(token);

        Integer userId = jwtUtil.extractUserId(oldRefreshToken);
        Employee emp = employeeRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return generateTokens(emp);
    }
}
