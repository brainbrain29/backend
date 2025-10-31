package com.pandora.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        Object adminId = session.getAttribute("adminId");
        
        // 如果未登录，重定向到登录页
        if (adminId == null) {
            response.sendRedirect("/admin/login");
            return false;
        }
        
        return true;
    }
}
