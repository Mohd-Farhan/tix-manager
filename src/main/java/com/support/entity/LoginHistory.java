package com.support.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * ==============================================================================================
 * ENTITY: LoginHistory (Authentication & Session Audit Trail)
 * ==============================================================================================
 * 
 * Dedicated security table tracking user authentication lifecycle:
 * - Login timestamps, client IP address (supporting X-Forwarded-For proxies), user agent.
 * - Authentication outcome (SUCCESS, FAILED_BAD_CREDENTIALS).
 * - Explicit logout timestamp when sessions terminate.
 */
@Entity
@Table(name = "login_history", indexes = {
    @Index(name = "idx_login_username", columnList = "username, login_time DESC"),
    @Index(name = "idx_login_time", columnList = "login_time DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, length = 100)
    private String username;

    @Column(name = "status", nullable = false, length = 50)
    private String status; // 'SUCCESS', 'FAILED_BAD_CREDENTIALS'

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "login_time", nullable = false, updatable = false)
    private LocalDateTime loginTime;

    @Column(name = "logout_time")
    private LocalDateTime logoutTime;

    @PrePersist
    protected void onCreate() {
        this.loginTime = LocalDateTime.now();
    }
}
