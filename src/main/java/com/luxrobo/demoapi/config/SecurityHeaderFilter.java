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

        // MIME 스니핑 방지
        response.setHeader("Content-Type", "application/json");

        chain.doFilter(request, servletResponse);
    }
}
