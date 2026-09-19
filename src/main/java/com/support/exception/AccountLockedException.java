package com.support.exception;

import lombok.Getter;

/**
 * ==============================================================================================
 * EXCEPTION: AccountLockedException (OWASP ASVS v4.0 §2.2.1 Brute-Force Rejection)
 * ==============================================================================================
 * 
 * WHY THIS IS USED:
 * Thrown when an IP address or username exceeds the maximum permitted consecutive failed
 * authentication attempts within the evaluation window. Mapped to HTTP 429 Too Many Requests.
 */
@Getter
public class AccountLockedException extends RuntimeException {

    private final int lockoutMinutes;

    public AccountLockedException(String message, int lockoutMinutes) {
        super(message);
        this.lockoutMinutes = lockoutMinutes;
    }
}
