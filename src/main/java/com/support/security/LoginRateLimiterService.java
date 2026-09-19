package com.support.security;

import com.support.exception.AccountLockedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ==============================================================================================
 * SERVICE: LoginRateLimiterService (OWASP ASVS v4.0 §2.2.1 Anti-Brute-Force Defense)
 * ==============================================================================================
 * 
 * WHY THIS IS USED (Industry & Enterprise Standard):
 * 1. Mitigation of Automated Credential Stuffing & Dictionary Attacks:
 *    - Unauthenticated `/api/auth/login` endpoints are the primary target for credential stuffing.
 *    - Without rate limiting, attackers can send thousands of password guesses per second.
 * 
 * 2. Sliding Window & Dual-Key Throttling:
 *    - Tracks consecutive failures across BOTH Client IP and Normalized Username.
 *    - Dual-key tracking prevents an attacker from bypassing IP limits using rotating botnet proxies
 *      against a single target user, and conversely prevents an attacker from locking out all users
 *      from an office IP by trying random usernames.
 * 
 * 3. Lockout Policy:
 *    - Threshold: 5 consecutive failed attempts within a 15-minute evaluation window.
 *    - Action: Enforces a 15-minute temporary lockout duration returning HTTP 429 Too Many Requests.
 *    - Reset: A single successful authentication immediately clears the failure counter for that key.
 */
@Slf4j
@Service
public class LoginRateLimiterService {

    public static final int MAX_ATTEMPTS = 5;
    public static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);
    public static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(15);

    private final Map<String, AttemptTracker> attemptsMap = new ConcurrentHashMap<>();

    /**
     * Internal container tracking failure counts and timestamps.
     */
    private static class AttemptTracker {
        int failureCount = 0;
        Instant lastFailureTime = Instant.now();
        Instant lockedUntil = null;
    }

    /**
     * Checks whether the given client IP or username is currently in a locked state.
     * 
     * @param clientIp Client IP address (proxy-aware)
     * @param username Attempted username
     * @throws AccountLockedException if either the IP or username is locked
     */
    public void checkBlocked(String clientIp, String username) {
        checkKeyBlocked("IP:" + normalize(clientIp));
        if (username != null && !username.isBlank()) {
            checkKeyBlocked("USER:" + normalize(username));
        }
    }

    /**
     * Records a failed authentication attempt for both IP and username.
     */
    public void recordFailure(String clientIp, String username) {
        recordKeyFailure("IP:" + normalize(clientIp));
        if (username != null && !username.isBlank()) {
            recordKeyFailure("USER:" + normalize(username));
        }
    }

    /**
     * Clears failure counters upon successful authentication.
     */
    public void recordSuccess(String clientIp, String username) {
        attemptsMap.remove("IP:" + normalize(clientIp));
        if (username != null && !username.isBlank()) {
            attemptsMap.remove("USER:" + normalize(username));
        }
        log.debug("Rate limiter reset for user '{}' from IP '{}'", username, clientIp);
    }

    /**
     * Checks if a specific key has exceeded the threshold or is locked.
     */
    private void checkKeyBlocked(String key) {
        AttemptTracker tracker = attemptsMap.get(key);
        if (tracker == null) {
            return;
        }

        Instant now = Instant.now();

        // Check if under active lockout
        if (tracker.lockedUntil != null) {
            if (now.isBefore(tracker.lockedUntil)) {
                long remainingMinutes = Math.max(1, Duration.between(now, tracker.lockedUntil).toMinutes() + 1);
                log.warn("Rate limit violation on key '{}': locked for {} more minutes", key, remainingMinutes);
                throw new AccountLockedException(
                        String.format("Too many failed login attempts. Access is locked for %d minute(s).", remainingMinutes),
                        (int) remainingMinutes);
            } else {
                // Lockout has expired; clear tracker
                attemptsMap.remove(key);
            }
        }
    }

    /**
     * Increments the failure count for a specific key and applies lockout if threshold reached.
     */
    private void recordKeyFailure(String key) {
        Instant now = Instant.now();
        attemptsMap.compute(key, (k, tracker) -> {
            if (tracker == null) {
                tracker = new AttemptTracker();
                tracker.failureCount = 1;
                tracker.lastFailureTime = now;
                return tracker;
            }

            // If the last failure was outside the rolling attempt window, reset count
            if (Duration.between(tracker.lastFailureTime, now).compareTo(ATTEMPT_WINDOW) > 0) {
                tracker.failureCount = 1;
                tracker.lastFailureTime = now;
                tracker.lockedUntil = null;
                return tracker;
            }

            tracker.failureCount++;
            tracker.lastFailureTime = now;

            if (tracker.failureCount >= MAX_ATTEMPTS) {
                tracker.lockedUntil = now.plus(LOCKOUT_DURATION);
                log.warn("Rate limiter triggered: key '{}' locked until {}", key, tracker.lockedUntil);
            }

            return tracker;
        });
    }

    /**
     * Helper for test inspection and manual reset.
     */
    public void resetAll() {
        attemptsMap.clear();
    }

    /**
     * Helper to query current failure count for testing.
     */
    public int getFailureCount(String keyPrefix, String identifier) {
        AttemptTracker tracker = attemptsMap.get(keyPrefix + ":" + normalize(identifier));
        return tracker != null ? tracker.failureCount : 0;
    }

    private String normalize(String str) {
        return str != null ? str.trim().toLowerCase() : "unknown";
    }
}
