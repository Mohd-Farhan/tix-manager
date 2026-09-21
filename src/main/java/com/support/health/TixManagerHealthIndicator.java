package com.support.health;

import com.support.config.AsyncConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * ==============================================================================================
 * HEALTH INDICATOR: TixManagerHealthIndicator (Kubernetes Liveness & Readiness Probes)
 * ==============================================================================================
 * 
 * WHY THIS IS USED (Cloud-Native & Container Orchestration Standards):
 * 1. Kubernetes Readiness Probes (/actuator/health/readiness):
 *    - Validates that the application is fully initialized and capable of serving user traffic.
 *    - If the database connection is saturated or unreachable, this indicator reports DOWN,
 *      instructing Kubernetes Service routing to stop directing ingress traffic to this pod.
 * 
 * 2. Kubernetes Liveness Probes (/actuator/health/liveness):
 *    - Detects unrecoverable deadlocks or corrupted internal state where container restart is mandatory.
 * 
 * 3. Observability of Asynchronous Subsystems:
 *    - Reports the live capacity, queue occupancy, and active thread count of `auditExecutor`.
 *    - Allows APM monitors (Prometheus, Datadog) to alert before task queues overflow.
 */
@Slf4j
@Component
public class TixManagerHealthIndicator implements HealthIndicator {

    @Autowired
    private DataSource dataSource;

    @Autowired
    @Qualifier(AsyncConfig.AUDIT_EXECUTOR)
    private Executor auditExecutor;

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("service", "TixManager API");
        details.put("status", "OPERATIONAL");

        // 1. Check active database connectivity
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(2)) {
                log.warn("Database connection check failed: connection is invalid");
                return Health.down().withDetail("database", "CONNECTION_INVALID").build();
            }
            details.put("database", "CONNECTED");
            details.put("databaseProduct", conn.getMetaData().getDatabaseProductName());
        } catch (Exception ex) {
            log.error("Database health check error: {}", ex.getMessage(), ex);
            return Health.down(ex).withDetail("database", "DISCONNECTED").build();
        }

        // 2. Check async telemetry thread pool health
        if (auditExecutor instanceof ThreadPoolTaskExecutor threadPool) {
            Map<String, Object> poolMetrics = new LinkedHashMap<>();
            poolMetrics.put("activeCount", threadPool.getActiveCount());
            poolMetrics.put("poolSize", threadPool.getPoolSize());
            poolMetrics.put("corePoolSize", threadPool.getCorePoolSize());
            poolMetrics.put("maxPoolSize", threadPool.getMaxPoolSize());
            poolMetrics.put("queueSize", threadPool.getQueueSize());
            poolMetrics.put("queueRemainingCapacity", threadPool.getThreadPoolExecutor().getQueue().remainingCapacity());
            details.put("asyncAuditExecutor", poolMetrics);
        }

        return Health.up().withDetails(details).build();
    }
}
