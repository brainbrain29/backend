package com.pandora.backend.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {
    private static final String SECRET = "PandoraSuperSecretKey1234567890Pandora!"; // 固定密钥字符串
    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
    private final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 60; // 1小时
    private final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7; // 7天

    /**
     * 生成 Access Token（包含 userId 和 position）
     * 前端可以解码 Token 获取权限信息用于 UI 渲染
     */
    public String generateAccessToken(Integer userId, Byte position) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("position", position) // 添加权限信息
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Integer userId) {
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            // Token 已过期
            System.err.println("Token 已过期: " + e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            // Token 格式错误
            System.err.println("Token 格式错误: " + e.getMessage());
            return false;
        } catch (SignatureException e) {
            // 签名验证失败
            System.err.println("Token 签名验证失败: " + e.getMessage());
            return false;
        } catch (JwtException e) {
            // 其他 JWT 异常
            System.err.println("Token 验证失败: " + e.getMessage());
            return false;
        }
    }

    public Integer extractUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return Integer.valueOf(claims.getSubject());
    }

    /**
     * 从 Token 中提取 position
     */
    public Byte extractPosition(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        Object position = claims.get("position");
        if (position == null) {
            return 3; // 默认普通员工
        }
        // 处理不同类型：可能是 Integer 或 Byte
        if (position instanceof Number) {
            return ((Number) position).byteValue();
        }
        return 3;
    }

    /**
     * 从 Token 中提取所有信息
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
