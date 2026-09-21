package com.support.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ==============================================================================================
 * AUTOMATED TESTING SUITE: AsyncConfigTest
 * ==============================================================================================
 * 
 * Verifies that:
 * 1. The dedicated `auditExecutor` ThreadPoolTaskExecutor is correctly configured with bounded limits.
 * 2. Asynchronous tasks run on worker threads with the prefix "async-audit-".
 * 3. MDC trace context (`traceId`) is propagated across threads via MdcTaskDecorator.
 * 4. Spring SecurityContext is propagated across thread boundaries.
 */
@SpringBootTest
class AsyncConfigTest {

    @Autowired
    @Qualifier(AsyncConfig.AUDIT_EXECUTOR)
    private Executor auditExecutor;

    @Test
    @DisplayName("auditExecutor — Bean initializes with bounded thread pool configuration")
    void testAuditExecutorConfiguration() {
        assertThat(auditExecutor).isInstanceOf(ThreadPoolTaskExecutor.class);

        ThreadPoolTaskExecutor threadPool = (ThreadPoolTaskExecutor) auditExecutor;
        assertThat(threadPool.getCorePoolSize()).isEqualTo(4);
        assertThat(threadPool.getMaxPoolSize()).isEqualTo(16);
        assertThat(threadPool.getQueueCapacity()).isEqualTo(500);
        assertThat(threadPool.getThreadNamePrefix()).isEqualTo("async-audit-");
    }

    @Test
    @DisplayName("auditExecutor — Tasks execute on background thread with propagated MDC and SecurityContext")
    void testMdcAndSecurityContextPropagation() throws Exception {
        MDC.put("traceId", "test-trace-12345");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("asyncUser", "password"));

        CompletableFuture<String[]> executionContextFuture = new CompletableFuture<>();

        auditExecutor.execute(() -> {
            String threadName = Thread.currentThread().getName();
            String traceId = MDC.get("traceId");
            String authName = SecurityContextHolder.getContext().getAuthentication() != null
                    ? SecurityContextHolder.getContext().getAuthentication().getName()
                    : null;
            executionContextFuture.complete(new String[]{threadName, traceId, authName});
        });

        String[] results = executionContextFuture.get(5, TimeUnit.SECONDS);

        assertThat(results[0]).startsWith("async-audit-");
        assertThat(results[1]).isEqualTo("test-trace-12345");
        assertThat(results[2]).isEqualTo("asyncUser");

        MDC.clear();
        SecurityContextHolder.clearContext();
    }
}
