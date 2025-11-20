package com.pandora.backend.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Handle access denied (403) and log user and request information.
 */
@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final AccessDeniedException accessDeniedException) throws IOException, ServletException {

        final Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        Integer userId = null;
        if (authentication != null && authentication.getPrincipal() instanceof Integer) {
            userId = (Integer) authentication.getPrincipal();
        }

        final String httpMethod = request.getMethod();
        final String requestUri = request.getRequestURI();

        log.warn("Access denied - userId: {}, method: {}, uri: {}",
                userId, httpMethod, requestUri);

        // Check if response is already committed (e.g., SSE connection closed)
        if (response.isCommitted()) {
            log.debug("Response already committed, skipping error response for uri: {}", requestUri);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"Access denied\"}");
    }
}
