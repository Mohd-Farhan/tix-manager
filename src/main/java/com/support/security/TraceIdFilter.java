package com.support.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * ==============================================================================================
 * SERVLET FILTER: TraceIdFilter
 * ==============================================================================================
 * Industry standard request correlation & HTTP access logging filter.
 * 
 * 1. Assigns a unique traceId per request (from incoming 'X-Correlation-ID' header or random UUID).
 * 2. Injects traceId into SLF4J MDC for logging consistency across threads.
 * 3. Adds 'X-Correlation-ID' response header for client-side tracing.
 * 4. Logs HTTP method, URI, response status, and duration in ms.
 * 5. Clears MDC in finally block to prevent thread pool memory leaks.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String MDC_TRACE_ID_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String traceId = request.getHeader(CORRELATION_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_TRACE_ID_KEY, traceId);
        response.setHeader(CORRELATION_ID_HEADER, traceId);

        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            String uri = request.getRequestURI();

            if (uri.startsWith("/actuator") || uri.startsWith("/h2-console")) {
                log.debug("HTTP {} {} -> {} ({}ms)", request.getMethod(), uri, response.getStatus(), duration);
            } else {
                log.info("HTTP {} {} -> {} ({}ms)", request.getMethod(), uri, response.getStatus(), duration);
            }

            MDC.clear();
        }
    }
}
