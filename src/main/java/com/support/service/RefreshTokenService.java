package com.support.service;

import com.support.entity.RefreshToken;
import com.support.entity.User;
import com.support.exception.RefreshTokenException;
import com.support.repository.RefreshTokenRepository;
import com.support.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ==============================================================================================
 * SERVICE: RefreshTokenService (Session Lifecycle & Real-Time Token Rotation/Revocation)
 * ==============================================================================================
 * 
 * WHY THIS ARCHITECTURE:
 * 1. Refresh Token Rotation (OWASP ASVS §3.5):
 *    - Each time a refresh token is used to issue a new access token, the presented refresh token
 *      is burned (`revoked = true`) and a brand new refresh token is issued.
 * 
 * 2. Automatic Breach Detection & Compromise Mitigation:
 *    - If an attacker intercepts a used refresh token and attempts to replay it, the system detects
 *      that a burned token was presented (`isRevoked() == true`). It instantly revokes ALL active
 *      sessions for that user account across all devices and invalidates their existing access tokens.
 * 
 * 3. Zero-Window Real-Time Revocation:
 *    - When sessions are terminated, `user.setLastLogoutAt(LocalDateTime.now())` is committed.
 *      The `JwtAuthenticationFilter` validates `issuedAt` against `lastLogoutAt` on every request,
 *      instantly terminating access tokens without waiting for the 15-minute expiration.
 */
@Slf4j
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final long refreshExpirationMs;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            @Value("${jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    /**
     * Issues a new 7-day cryptographic refresh token for the authenticated user.
     */
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        String tokenValue = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusMillis(refreshExpirationMs);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenValue)
                .expiryDate(expiryDate)
                .revoked(false)
                .createdAt(Instant.now())
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.debug("Created refresh token id={} for user '{}' (expires: {})", saved.getId(), user.getUsername(), expiryDate);
        return saved;
    }

    /**
     * Validates and rotates an existing refresh token into a new refresh token pair.
     * Implements OWASP Token Reuse Detection.
     */
    @Transactional
    public RefreshToken rotateRefreshToken(String requestToken) {
        RefreshToken existingToken = refreshTokenRepository.findByToken(requestToken)
                .orElseThrow(() -> new RefreshTokenException("Invalid refresh token."));

        User user = existingToken.getUser();

        // Security check 1: Token reuse detection
        if (existingToken.isRevoked()) {
            log.error("SECURITY BREACH ALERT: Revoked refresh token reuse attempted for user '{}'! Invalidating all user sessions.", user.getUsername());
            revokeAllUserTokens(user);
            throw new RefreshTokenException("Refresh token was previously revoked. For your security, all active sessions have been invalidated. Please sign in again.");
        }

        // Security check 2: Expiration
        if (existingToken.getExpiryDate().isBefore(Instant.now())) {
            existingToken.setRevoked(true);
            refreshTokenRepository.save(existingToken);
            log.warn("Expired refresh token presented for user '{}'", user.getUsername());
            throw new RefreshTokenException("Refresh token has expired. Please sign in again.");
        }

        // Security check 3: User account active status
        if (user.isDeleted()) {
            existingToken.setRevoked(true);
            refreshTokenRepository.save(existingToken);
            throw new RefreshTokenException("User account is deactivated.");
        }

        // Rotation: Issue new token and burn old token
        RefreshToken newRefreshToken = createRefreshToken(user);

        existingToken.setRevoked(true);
        existingToken.setReplacedByToken(newRefreshToken.getToken());
        refreshTokenRepository.save(existingToken);

        log.info("Rotated refresh token for user '{}'. Old token burned.", user.getUsername());
        return newRefreshToken;
    }

    /**
     * Real-time revocation of all sessions for a user (called on password change, deactivation, or global logout).
     */
    @Transactional
    public void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllUserTokens(user);
        user.setLastLogoutAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Revoked all refresh tokens and set lastLogoutAt for user '{}'", user.getUsername());
    }

    /**
     * Revokes a single refresh token and terminates access window (standard user device logout).
     */
    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            User u = rt.getUser();
            u.setLastLogoutAt(LocalDateTime.now());
            userRepository.save(u);
            log.info("Revoked refresh token id={} and updated lastLogoutAt for user '{}'", rt.getId(), u.getUsername());
        });
    }
}
