package com.support.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * ==============================================================================================
 * CONFIGURATION: AsyncConfig (Enterprise Asynchronous Execution & Scalability)
 * ==============================================================================================
 * 
 * WHY THIS IS USED (Industry Best Practices & High-Throughput Scalability):
 * 1. Non-Blocking I/O Decoupling:
 *    - In synchronous architectures, audit log writes and login history tracking execute on the
 *      active HTTP servlet thread. A database latency spike directly degrades client response times.
 *    - Offloading non-critical telemetry to a background thread pool returns HTTP 200/201 responses
 *      to the client immediately (sub-millisecond latency).
 * 
 * 2. Bounded Thread Pool Sizing (Preventing Thread Exhaustion):
 *    - Core pool: 4 threads (handles baseline concurrent audit operations).
 *    - Max pool: 16 threads (scales gracefully under traffic spikes).
 *    - Queue capacity: 500 tasks (buffers bursts without consuming excessive heap memory).
 *    - Rejection policy: CallerRunsPolicy (ensures zero telemetry data loss under severe backpressure;
 *      if the queue fills, the caller thread executes the task synchronously).
 * 
 * 3. Context Propagation (Distributed Traceability & Security Across Threads):
 *    - In multi-threaded Spring architectures, Logback MDC (Mapped Diagnostic Context) and
 *      SecurityContext are thread-local.
 *    - A custom TaskDecorator copies the caller thread's MDC context map (containing `traceId`)
 *      and SecurityContext to the async worker thread, keeping distributed log traces contiguous
 *      and actor credentials accessible across async boundaries.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String AUDIT_EXECUTOR = "auditExecutor";

    @Bean(name = AUDIT_EXECUTOR)
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("async-audit-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // Propagate both MDC (traceId) and SecurityContext across thread boundaries
        executor.setTaskDecorator(new AsyncSecurityMdcTaskDecorator());
        
        executor.initialize();
        log.info("Initialized auditExecutor thread pool: core=4, max=16, queue=500");
        return executor;
    }

    /**
     * Propagates MDC (traceId) and SecurityContext from the caller thread to the worker thread.
     */
    public static class AsyncSecurityMdcTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            SecurityContext securityContext = SecurityContextHolder.getContext();
            Map<String, String> contextMap = MDC.getCopyOfContextMap();
            return () -> {
                SecurityContext originalSecurityContext = SecurityContextHolder.getContext();
                try {
                    if (securityContext != null) {
                        SecurityContextHolder.setContext(securityContext);
                    }
                    if (contextMap != null) {
                        MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    SecurityContextHolder.setContext(originalSecurityContext);
                    MDC.clear();
                }
            };
        }
    }
}
