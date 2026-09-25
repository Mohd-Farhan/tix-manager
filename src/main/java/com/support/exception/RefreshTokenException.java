package com.support.exception;

/**
 * ==============================================================================================
 * EXCEPTION: RefreshTokenException (HTTP 401 Unauthorized)
 * ==============================================================================================
 * 
 * Thrown when a refresh token is expired, revoked, replayed, or invalid.
 */
public class RefreshTokenException extends RuntimeException {
    public RefreshTokenException(String message) {
        super(message);
    }
}
