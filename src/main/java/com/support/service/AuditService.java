package com.support.service;

import com.support.entity.AuditLog;
import com.support.entity.LoginHistory;
import com.support.repository.AuditLogRepository;
import com.support.repository.LoginHistoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * ==============================================================================================
 * SERVICE: AuditService
 * ==============================================================================================
 * 
 * Central manager for persisting entity audit logs and authentication history.
 */
@Slf4j
@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private LoginHistoryRepository loginHistoryRepository;

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
            log.debug("AuditLog saved: [{} #{}] {} by {}", entityName, entityId, action, performedBy);
        } catch (Exception e) {
            log.error("Failed to persist audit log for [{} #{}]: {}", entityName, entityId, e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginSuccess(String username, HttpServletRequest request) {
        try {
            LoginHistory entry = LoginHistory.builder()
                    .username(username)
                    .status("SUCCESS")
                    .ipAddress(extractClientIp(request))
                    .userAgent(extractUserAgent(request))
                    .traceId(MDC.get("traceId"))
                    .build();

            loginHistoryRepository.save(entry);
            log.debug("LoginHistory recorded for user '{}'", username);
        } catch (Exception e) {
            log.error("Failed to persist login history for user '{}': {}", username, e.getMessage(), e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginFailure(String username, HttpServletRequest request, String reason) {
        try {
            LoginHistory entry = LoginHistory.builder()
                    .username(username != null ? username : "UNKNOWN")
                    .status("FAILED: " + (reason != null ? reason : "BAD_CREDENTIALS"))
                    .ipAddress(extractClientIp(request))
                    .userAgent(extractUserAgent(request))
                    .traceId(MDC.get("traceId"))
                    .build();

            loginHistoryRepository.save(entry);
            log.debug("Login failure history recorded for user '{}'", username);
        } catch (Exception e) {
            log.error("Failed to persist login failure for user '{}': {}", username, e.getMessage(), e);
        }
    }

    @Transactional
    public void recordLogout(String username) {
        try {
            loginHistoryRepository.findFirstByUsernameAndLogoutTimeIsNullOrderByLoginTimeDesc(username)
                    .ifPresent(entry -> {
                        entry.setLogoutTime(LocalDateTime.now());
                        loginHistoryRepository.save(entry);
                        log.debug("Logout recorded for user '{}' (login ID: {})", username, entry.getId());
                    });
        } catch (Exception e) {
            log.error("Failed to record logout for user '{}': {}", username, e.getMessage(), e);
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        String xf = request.getHeader("X-Forwarded-For");
        if (xf != null && !xf.isBlank()) {
            return xf.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest request) {
        if (request == null) return "unknown";
        String ua = request.getHeader("User-Agent");
        return (ua != null && ua.length() > 255) ? ua.substring(0, 255) : ua;
    }
}
