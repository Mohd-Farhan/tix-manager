package com.support.service;

import com.support.repository.AuditLogRepository;
import com.support.repository.LoginHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditPurgeServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private LoginHistoryRepository loginHistoryRepository;

    @InjectMocks
    private AuditPurgeService auditPurgeService;

    @BeforeEach
    void setUp() {
        // Point self-invocation to the mocked instance for unit test
        ReflectionTestUtils.setField(auditPurgeService, "self", auditPurgeService);
    }

    @Test
    @DisplayName("purgeAuditLogsChunk — Returns 0 when no old IDs found")
    void testPurgeAuditLogsChunk_Empty() {
        when(auditLogRepository.findOldAuditLogIds(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(Collections.emptyList());

        int deleted = auditPurgeService.purgeAuditLogsChunk(LocalDateTime.now(), 100);

        assertThat(deleted).isEqualTo(0);
        verify(auditLogRepository, never()).deleteBatchByIds(any());
    }

    @Test
    @DisplayName("purgeAuditLogsChunk — Deletes matching IDs and returns count")
    void testPurgeAuditLogsChunk_Success() {
        List<Long> mockIds = List.of(1L, 2L, 3L);
        when(auditLogRepository.findOldAuditLogIds(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(mockIds);
        when(auditLogRepository.deleteBatchByIds(mockIds)).thenReturn(3);

        int deleted = auditPurgeService.purgeAuditLogsChunk(LocalDateTime.now(), 100);

        assertThat(deleted).isEqualTo(3);
        verify(auditLogRepository, times(1)).deleteBatchByIds(mockIds);
    }

    @Test
    @DisplayName("purgeAuditLogs — Loops through multiple batches until last batch is smaller than batchSize")
    void testPurgeAuditLogs_MultipleBatches() {
        List<Long> batch1 = List.of(1L, 2L);
        List<Long> batch2 = List.of(3L);

        when(auditLogRepository.findOldAuditLogIds(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(batch1)
                .thenReturn(batch2);

        when(auditLogRepository.deleteBatchByIds(batch1)).thenReturn(2);
        when(auditLogRepository.deleteBatchByIds(batch2)).thenReturn(1);

        int totalDeleted = auditPurgeService.purgeAuditLogs(90, 2);

        // 2 in first batch + 1 in second batch = 3 total
        assertThat(totalDeleted).isEqualTo(3);
        verify(auditLogRepository, times(2)).deleteBatchByIds(any());
    }

    @Test
    @DisplayName("purgeLoginHistoryChunk — Deletes matching IDs and returns count")
    void testPurgeLoginHistoryChunk_Success() {
        List<Long> mockIds = List.of(10L, 20L);
        when(loginHistoryRepository.findOldLoginHistoryIds(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(mockIds);
        when(loginHistoryRepository.deleteBatchByIds(mockIds)).thenReturn(2);

        int deleted = auditPurgeService.purgeLoginHistoryChunk(LocalDateTime.now(), 50);

        assertThat(deleted).isEqualTo(2);
        verify(loginHistoryRepository, times(1)).deleteBatchByIds(mockIds);
    }

    @Test
    @DisplayName("purgeLoginHistory — Completes single-batch purge")
    void testPurgeLoginHistory_SingleBatch() {
        List<Long> batch = List.of(10L);
        when(loginHistoryRepository.findOldLoginHistoryIds(any(LocalDateTime.class), any(Pageable.class)))
                .thenReturn(batch);
        when(loginHistoryRepository.deleteBatchByIds(batch)).thenReturn(1);

        int totalDeleted = auditPurgeService.purgeLoginHistory(60, 100);

        assertThat(totalDeleted).isEqualTo(1);
        verify(loginHistoryRepository, times(1)).deleteBatchByIds(batch);
    }
}
