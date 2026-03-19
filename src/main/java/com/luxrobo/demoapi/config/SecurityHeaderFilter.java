package com.luxrobo.demoapi.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(0)
public class SecurityHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // XSS 방어
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // 클릭재킹 방어
        response.setHeader("X-Frame-Options", "DENY");

        // HTTPS 강제
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // CSP
        response.setHeader("Content-Security-Policy", "default-src 'self'");

        // Referrer 정보 누출 방지
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 브라우저 기능 제한
        response.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        chain.doFilter(request, servletResponse);
    }
}
