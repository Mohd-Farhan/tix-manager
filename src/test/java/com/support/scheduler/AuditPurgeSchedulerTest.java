package com.support.scheduler;

import com.support.service.AuditPurgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditPurgeSchedulerTest {

    @Mock
    private AuditPurgeService auditPurgeService;

    @InjectMocks
    private AuditPurgeScheduler auditPurgeScheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(auditPurgeScheduler, "auditRetentionDays", 90);
        ReflectionTestUtils.setField(auditPurgeScheduler, "loginHistoryRetentionDays", 60);
        ReflectionTestUtils.setField(auditPurgeScheduler, "purgeBatchSize", 1000);
    }

    @Test
    @DisplayName("executeScheduledPurge — Triggers purge for both audit_logs and login_history")
    void testExecuteScheduledPurge() {
        when(auditPurgeService.purgeAuditLogs(90, 1000)).thenReturn(25);
        when(auditPurgeService.purgeLoginHistory(60, 1000)).thenReturn(10);

        auditPurgeScheduler.executeScheduledPurge();

        verify(auditPurgeService, times(1)).purgeAuditLogs(90, 1000);
        verify(auditPurgeService, times(1)).purgeLoginHistory(60, 1000);
    }

    @Test
    @DisplayName("executeScheduledPurge — Handles exception gracefully without uncaught failure")
    void testExecuteScheduledPurge_HandlesException() {
        when(auditPurgeService.purgeAuditLogs(anyInt(), anyInt()))
                .thenThrow(new RuntimeException("DB Connection Timeout"));

        // Should not throw
        auditPurgeScheduler.executeScheduledPurge();

        verify(auditPurgeService, times(1)).purgeAuditLogs(90, 1000);
        verify(auditPurgeService, never()).purgeLoginHistory(anyInt(), anyInt());
    }
}
