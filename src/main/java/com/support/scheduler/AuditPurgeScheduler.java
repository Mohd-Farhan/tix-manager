package com.support.scheduler;

import com.support.service.AuditPurgeService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ==============================================================================================
 * SCHEDULER: AuditPurgeScheduler
 * ==============================================================================================
 * 
 * Production table purging background job.
 * Runs off-peak (default: 02:00 AM daily) to delete expired audit logs and login history.
 * Uses batch chunking to prevent table lock escalation and transaction log bloat.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.audit.purge.enabled", havingValue = "true", matchIfMissing = true)
public class AuditPurgeScheduler {

    @Autowired
    private AuditPurgeService auditPurgeService;

    @Value("${app.audit.retention-days:90}")
    private int auditRetentionDays;

    @Value("${app.audit.login-history-retention-days:60}")
    private int loginHistoryRetentionDays;

    @Value("${app.audit.purge-batch-size:1000}")
    private int purgeBatchSize;

    @Scheduled(cron = "${app.audit.purge.cron:0 0 2 * * ?}")
    public void executeScheduledPurge() {
        String purgeTraceId = "purge-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", purgeTraceId);
        long startTime = System.currentTimeMillis();

        try {
            log.info("Starting scheduled audit table purge (retention: {}d audit, {}d login, batchSize: {})...",
                    auditRetentionDays, loginHistoryRetentionDays, purgeBatchSize);

            int deletedAuditLogs = auditPurgeService.purgeAuditLogs(auditRetentionDays, purgeBatchSize);
            int deletedLoginHistory = auditPurgeService.purgeLoginHistory(loginHistoryRetentionDays, purgeBatchSize);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Scheduled audit table purge completed in {}ms. Summary: [audit_logs deleted: {}] [login_history deleted: {}]",
                    elapsed, deletedAuditLogs, deletedLoginHistory);
        } catch (Exception e) {
            log.error("Error occurred during scheduled audit table purge: {}", e.getMessage(), e);
        } finally {
            MDC.remove("traceId");
        }
    }
}
