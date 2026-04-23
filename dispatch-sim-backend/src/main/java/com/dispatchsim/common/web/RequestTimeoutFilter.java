package com.dispatchsim.common.web;

import com.dispatchsim.common.exception.TimeoutException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class RequestTimeoutFilter extends OncePerRequestFilter {

    private static final long REQUEST_TIMEOUT_MS = 10_000L;

    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startedAt = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long elapsed = System.currentTimeMillis() - startedAt;
            if (elapsed > REQUEST_TIMEOUT_MS && !response.isCommitted()) {
                handlerExceptionResolver.resolveException(
                        request,
                        response,
                        null,
                        new TimeoutException("请求处理超时，请稍后重试")
                );
            }
        }
    }
}
