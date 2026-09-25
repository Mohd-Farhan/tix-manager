package com.support.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * ==============================================================================================
 * ENTITY: RefreshToken (Long-lived Session Credential with Rotation & Revocation)
 * ==============================================================================================
 * 
 * WHY THIS IS USED (OWASP ASVS §3.5 & RFC 6749):
 * 1. Token Lifespan Separation:
 *    - Stateless access tokens are short-lived (15 min) to minimize exposure windows if intercepted.
 *    - Refresh tokens allow client sessions to persist for 7 days without repeatedly asking for passwords.
 * 
 * 2. Real-Time Revocation & Rotation:
 *    - When a refresh token is exchanged, it is burned and a new one is issued (Rotation).
 *    - If a revoked token is ever presented (token theft/replay), all active sessions for that user
 *      are immediately revoked.
 * 
 * 3. Fast Indexed Lookups:
 *    - B-Tree index on `token` guarantees O(1) retrieval during refresh handshakes.
 *    - B-Tree index on `user_id` allows immediate revocation of all user devices upon password change.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_val", columnList = "token", unique = true),
        @Index(name = "idx_refresh_token_user", columnList = "user_id"),
        @Index(name = "idx_refresh_token_expiry", columnList = "expiry_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 120)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    /**
     * Records the token that superseded this one during token rotation.
     * Essential for identifying token reuse anomalies.
     */
    @Column(name = "replaced_by_token", length = 120)
    private String replacedByToken;
}
