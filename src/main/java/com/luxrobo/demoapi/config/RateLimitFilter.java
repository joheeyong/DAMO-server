package com.luxrobo.demoapi.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(1)
public class RateLimitFilter implements Filter {

    private static final int MAX_BUCKETS = 10_000;

    private final Map<String, Bucket> buckets = new LinkedHashMap<>(MAX_BUCKETS, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Bucket> eldest) {
            return size() > MAX_BUCKETS;
        }
    };

    private Bucket createBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(100, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(1000, Duration.ofHours(1)))
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        // X-Forwarded-For 헤더는 클라이언트가 조작 가능하므로 remoteAddr만 사용
        // 리버스 프록시 환경에서는 프록시 설정(server.forward-headers-strategy=NATIVE)을 사용
        return request.getRemoteAddr();
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String clientIp = getClientIp(request);
        Bucket bucket;
        synchronized (buckets) {
            bucket = buckets.computeIfAbsent(clientIp, k -> createBucket());
        }

        if (bucket.tryConsume(1)) {
            chain.doFilter(servletRequest, servletResponse);
        } else {
            response.setStatus(429);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Too many requests. Please try again later.\"}");
        }
    }
}
