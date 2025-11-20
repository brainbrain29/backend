package com.pandora.backend.filter;

import com.pandora.backend.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import org.springframework.lang.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 1. 优先从 Authorization 头获取 token
            String token = null;
            String header = request.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                token = header.substring(7);
            }

            // 2. 如果 Authorization 头没有，尝试从 URL 参数获取（用于附件预览/下载）
            if (token == null || token.trim().isEmpty()) {
                String tokenParam = request.getParameter("token");
                if (tokenParam != null && !tokenParam.trim().isEmpty()) {
                    token = tokenParam;
                    log.debug("请求路径: {} - 从URL参数获取Token", request.getRequestURI());
                }
            }

            // 3. 如果两处都没有 token，直接放行（由 Spring Security 处理）
            if (token == null || token.trim().isEmpty()) {
                log.debug("请求路径: {} - 未提供 JWT Token", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            // 4. 验证 token
            if (!jwtUtil.validateToken(token)) {
                log.warn("请求路径: {} - JWT Token 无效或已过期", request.getRequestURI());
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Token 无效或已过期\"}");
                return;
            }

            // token 有效，解析出 userId 和 position
            Integer userId = jwtUtil.extractUserId(token);
            Byte position = jwtUtil.extractPosition(token);

            if (userId == null) {
                log.error("请求路径: {} - 无法从 Token 中提取 userId", request.getRequestURI());
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Token 格式错误\"}");
                return;
            }

            log.debug("请求路径: {} - 用户 {} 认证成功", request.getRequestURI(), userId);

            // 注入请求属性，供后续接口使用
            request.setAttribute("userId", userId);
            request.setAttribute("position", position);

            // 设置 Spring Security 的认证上下文
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userId, null, Collections.emptyList());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("JWT 认证过程中发生异常 - 请求路径: {}, 错误信息: {}",
                    request.getRequestURI(), e.getMessage(), e);

            // 清除可能存在的认证信息
            SecurityContextHolder.clearContext();

            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\":\"认证过程发生错误\"}");
        }
    }
}
