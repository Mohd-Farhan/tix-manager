package com.support.service;

import com.support.repository.AuditLogRepository;
import com.support.repository.LoginHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ==============================================================================================
 * SERVICE: AuditPurgeService
 * ==============================================================================================
 * 
 * WHY CHUNKED PURGING (Production Best Practice):
 * 1. Prevents Table Lock Escalation: Large single DELETE statements lock rows or entire pages,
 *    blocking concurrent application transactions.
 * 2. Prevents DB Log Exhaustion: Deleting millions of rows in one transaction exhausts database
 *    undo/rollback logs and WAL buffers.
 * 3. Atomic Batch Commits: Each chunk of size `batchSize` commits independently via REQUIRES_NEW.
 *    If interrupted, completed batches remain purged without rolling back the entire job.
 */
@Slf4j
@Service
public class AuditPurgeService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    @Lazy
    @Autowired
    private AuditPurgeService self;

    /**
     * Purge audit_logs records older than retentionDays in batches of batchSize.
     */
    public int purgeAuditLogs(int retentionDays, int batchSize) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        log.info("Starting audit_logs purge: older than {} days (cutoff: {}), batchSize: {}", retentionDays, cutoff, batchSize);

        int totalDeleted = 0;
        int deletedInBatch;
        do {
            deletedInBatch = self.purgeAuditLogsChunk(cutoff, batchSize);
            totalDeleted += deletedInBatch;
            if (deletedInBatch > 0) {
                log.debug("Purged {} audit_logs records (total so far: {})", deletedInBatch, totalDeleted);
            }
        } while (deletedInBatch == batchSize);

        log.info("Completed audit_logs purge. Total records deleted: {}", totalDeleted);
        return totalDeleted;
    }

    /**
     * Deletes a single batch of audit_logs within its own transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int purgeAuditLogsChunk(LocalDateTime cutoff, int batchSize) {
        List<Long> ids = auditLogRepository.findOldAuditLogIds(cutoff, PageRequest.of(0, batchSize));
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return auditLogRepository.deleteBatchByIds(ids);
    }

    /**
     * Purge login_history records older than retentionDays in batches of batchSize.
     */
    public int purgeLoginHistory(int retentionDays, int batchSize) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        log.info("Starting login_history purge: older than {} days (cutoff: {}), batchSize: {}", retentionDays, cutoff, batchSize);

        int totalDeleted = 0;
        int deletedInBatch;
        do {
            deletedInBatch = self.purgeLoginHistoryChunk(cutoff, batchSize);
            totalDeleted += deletedInBatch;
            if (deletedInBatch > 0) {
                log.debug("Purged {} login_history records (total so far: {})", deletedInBatch, totalDeleted);
            }
        } while (deletedInBatch == batchSize);

        log.info("Completed login_history purge. Total records deleted: {}", totalDeleted);
        return totalDeleted;
    }

    /**
     * Deletes a single batch of login_history within its own transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int purgeLoginHistoryChunk(LocalDateTime cutoff, int batchSize) {
        List<Long> ids = loginHistoryRepository.findOldLoginHistoryIds(cutoff, PageRequest.of(0, batchSize));
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return loginHistoryRepository.deleteBatchByIds(ids);
    }
}
