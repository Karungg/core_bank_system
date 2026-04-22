package com.miftah.core_bank_system.security;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.github.bucket4j.Bucket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String ip = getClientIp(request);

        Bucket bucket = null;
        if (uri.endsWith("/api/v1/auth/login")) {
            bucket = rateLimitingService.getLoginBucket(ip);
        } else if (uri.endsWith("/api/v1/auth/register")) {
            bucket = rateLimitingService.getRegisterBucket(ip);
        } else if (uri.endsWith("/api/v1/auth/refresh")) {
            bucket = rateLimitingService.getRefreshBucket(ip);
        }

        if (bucket != null) {
            if (bucket.tryConsume(1)) {
                filterChain.doFilter(request, response);
            } else {
                log.warn("Rate limit exceeded for IP: {} on {}", ip, uri);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\": \"Too Many Requests\", \"message\": \"Rate limit exceeded. Please try again later.\"}");
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
