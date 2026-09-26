package com.support.scheduler;

import com.support.service.SlaService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ==============================================================================================
 * SCHEDULER: SlaEscalationScheduler (Background SLA Resolution Deadline Watchdog)
 * ==============================================================================================
 * 
 * Periodically scans active tickets for SLA deadline breaches.
 * 
 * NOTE: Currently configured to run every 5 minutes (300000ms) for development and staging.
 * In high-volume production, this MUST be scheduled every 1 minute (60000ms) to ensure
 * instantaneous escalation and SLA violation containment.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.sla.escalation-enabled", havingValue = "true", matchIfMissing = true)
public class SlaEscalationScheduler {

    @Autowired
    private SlaService slaService;

    @Scheduled(fixedDelayString = "${app.sla.check-interval-ms:300000}")
    public void executeSlaCheck() {
        String traceId = "sla-" + UUID.randomUUID().toString().substring(0, 8);
        MDC.put("traceId", traceId);

        try {
            log.debug("Executing scheduled SLA breach watchdog...");
            int escalatedCount = slaService.checkAndEscalateBreachedTickets();
            if (escalatedCount > 0) {
                log.info("Scheduled SLA check completed: {} ticket(s) breached/escalated.", escalatedCount);
            }
        } catch (Exception e) {
            log.error("Error during scheduled SLA breach escalation: {}", e.getMessage(), e);
        } finally {
            MDC.remove("traceId");
        }
    }
}
