package com.support.service;

import com.support.config.AsyncConfig;
import com.support.entity.AuditLog;
import com.support.entity.LoginHistory;
import com.support.repository.AuditLogRepository;
import com.support.repository.LoginHistoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * ==============================================================================================
 * SERVICE: AuditService (High-Throughput Asynchronous Telemetry & Auditing)
 * ==============================================================================================
 * 
 * WHY ASYNCHRONOUS EXECUTION & SEPARATE TRANSACTIONS (@Async + REQUIRES_NEW):
 * 1. Non-Blocking Client Latency:
 *    - Persisting audit logs and login telemetry to the database is an observational concern,
 *      not part of the core business transaction (e.g. ticket triage or authentication).
 *    - Annotating with `@Async(AsyncConfig.AUDIT_EXECUTOR)` decouples write I/O to a bounded
 *      background thread pool, returning HTTP responses immediately to the client.
 * 
 * 2. Failure Isolation via Propagation.REQUIRES_NEW:
 *    - Audit operations execute in their own isolated transaction. Even if the outer business
 *      transaction fails, or if an audit insert hits a database contention issue, the business
 *      flow is neither polluted nor aborted.
 * 
 * 3. Thread-Safe Request Data Extraction:
 *    - HttpServletRequest instances are tied to the servlet container thread lifecycle and may
 *      be recycled before asynchronous worker threads execute. IP address and User-Agent headers
 *      are extracted synchronously before dispatching async tasks.
 */
@Slf4j
@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

    /**
     * Asynchronously records an entity lifecycle mutation (CREATE, UPDATE, DELETE, etc.).
     */
    @Async(AsyncConfig.AUDIT_EXECUTOR)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEntityChange(String entityName, Long entityId, String action, String performedBy, String details) {
        try {
            String traceId = MDC.get("traceId");
            if (performedBy == null || performedBy.isBlank()) {
                org.springframework.security.core.Authentication auth =
                        org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                    performedBy = auth.getName();
                } else {
                    performedBy = "SYSTEM";
                }
            }

            AuditLog auditLog = AuditLog.builder()
                    .entityName(entityName)
                    .entityId(entityId)
                    .action(action)
                    .performedBy(performedBy)
                    .traceId(traceId)
                    .details(details)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("AuditLog saved asynchronously: [{} #{}] {} by {}", entityName, entityId, action, performedBy);
        } catch (Exception e) {
            log.error("Failed to persist audit log for [{} #{}]: {}", entityName, entityId, e.getMessage(), e);
        }
    }

    /**
     * Synchronous entrypoint extracting IP and User-Agent before async persistence.
     */
    public void recordLoginSuccess(String username, HttpServletRequest request) {
        String ipAddress = extractClientIp(request);
        String userAgent = extractUserAgent(request);
        recordLoginSuccess(username, ipAddress, userAgent);
    }

    /**
     * Asynchronously persists successful authentication history.
     */
    @Async(AsyncConfig.AUDIT_EXECUTOR)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginSuccess(String username, String ipAddress, String userAgent) {
        try {
            LoginHistory entry = LoginHistory.builder()
                    .username(username)
                    .status("SUCCESS")
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .traceId(MDC.get("traceId"))
                    .build();

            loginHistoryRepository.save(entry);
            log.debug("LoginHistory recorded asynchronously for user '{}'", username);
        } catch (Exception e) {
            log.error("Failed to persist login history for user '{}': {}", username, e.getMessage(), e);
        }
    }

    /**
     * Synchronous entrypoint extracting IP and User-Agent before async persistence.
     */
    public void recordLoginFailure(String username, HttpServletRequest request, String reason) {
        String ipAddress = extractClientIp(request);
        String userAgent = extractUserAgent(request);
        recordLoginFailure(username, ipAddress, userAgent, reason);
    }

    /**
     * Asynchronously persists failed authentication history with error reason.
     */
    @Async(AsyncConfig.AUDIT_EXECUTOR)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginFailure(String username, String ipAddress, String userAgent, String reason) {
        try {
            LoginHistory entry = LoginHistory.builder()
                    .username(username != null ? username : "UNKNOWN")
                    .status("FAILED: " + (reason != null ? reason : "BAD_CREDENTIALS"))
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .traceId(MDC.get("traceId"))
                    .build();

            loginHistoryRepository.save(entry);
            log.debug("Login failure history recorded asynchronously for user '{}'", username);
        } catch (Exception e) {
            log.error("Failed to persist login failure for user '{}': {}", username, e.getMessage(), e);
        }
    }

    /**
     * Records session logout and computes session duration timestamp.
     */
    @Async(AsyncConfig.AUDIT_EXECUTOR)
    @Transactional
    public void recordLogout(String username) {
        try {
            loginHistoryRepository.findFirstByUsernameAndLogoutTimeIsNullOrderByLoginTimeDesc(username)
                    .ifPresent(entry -> {
                        entry.setLogoutTime(LocalDateTime.now());
                        loginHistoryRepository.save(entry);
                        log.debug("Logout recorded asynchronously for user '{}' (login ID: {})", username, entry.getId());
                    });
        } catch (Exception e) {
            log.error("Failed to record logout for user '{}': {}", username, e.getMessage(), e);
        }
    }

    public String extractClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public String extractUserAgent(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ua = request.getHeader("User-Agent");
        return (ua != null && ua.length() > 255) ? ua.substring(0, 255) : ua;
    }
}
