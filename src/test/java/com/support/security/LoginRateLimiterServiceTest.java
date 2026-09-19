package com.support.security;

import com.support.exception.AccountLockedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ==============================================================================================
 * UNIT TESTS: LoginRateLimiterService (OWASP ASVS v4.0 Anti-Brute-Force Test Suite)
 * ==============================================================================================
 */
class LoginRateLimiterServiceTest {

    private LoginRateLimiterService rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new LoginRateLimiterService();
        rateLimiter.resetAll();
    }

    @Test
    @DisplayName("checkBlocked — Allows login when below failure threshold")
    void testCheckBlocked_AllowsBelowThreshold() {
        String ip = "192.168.1.100";
        String username = "alice";

        for (int i = 0; i < LoginRateLimiterService.MAX_ATTEMPTS - 1; i++) {
            rateLimiter.recordFailure(ip, username);
        }

        assertThat(rateLimiter.getFailureCount("IP", ip)).isEqualTo(4);
        assertThat(rateLimiter.getFailureCount("USER", username)).isEqualTo(4);
        assertThatCode(() -> rateLimiter.checkBlocked(ip, username)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("checkBlocked — Throws AccountLockedException when threshold reached")
    void testCheckBlocked_LocksOutAtThreshold() {
        String ip = "192.168.1.101";
        String username = "bob";

        for (int i = 0; i < LoginRateLimiterService.MAX_ATTEMPTS; i++) {
            rateLimiter.recordFailure(ip, username);
        }

        assertThat(rateLimiter.getFailureCount("IP", ip)).isEqualTo(5);
        assertThatThrownBy(() -> rateLimiter.checkBlocked(ip, username))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("Too many failed login attempts");
    }

    @Test
    @DisplayName("recordSuccess — Clears failure count on successful authentication")
    void testRecordSuccess_ClearsCounter() {
        String ip = "192.168.1.102";
        String username = "charlie";

        rateLimiter.recordFailure(ip, username);
        rateLimiter.recordFailure(ip, username);
        assertThat(rateLimiter.getFailureCount("IP", ip)).isEqualTo(2);

        rateLimiter.recordSuccess(ip, username);

        assertThat(rateLimiter.getFailureCount("IP", ip)).isEqualTo(0);
        assertThat(rateLimiter.getFailureCount("USER", username)).isEqualTo(0);
        assertThatCode(() -> rateLimiter.checkBlocked(ip, username)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Dual-Key Tracking — Blocks attacker attempting brute force across different IPs")
    void testDualKeyTracking_UserBlockedAcrossIps() {
        String username = "target_user";

        // Attacker uses 5 different IPs against same user
        for (int i = 1; i <= 5; i++) {
            rateLimiter.recordFailure("10.0.0." + i, username);
        }

        // Target user is locked regardless of fresh IP
        assertThatThrownBy(() -> rateLimiter.checkBlocked("10.0.0.99", username))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("Too many failed login attempts");
    }
}
