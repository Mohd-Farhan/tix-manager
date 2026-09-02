package com.support.exception;

/**
 * ==============================================================================================
 * DOMAIN EXCEPTION: InvalidOperationException (HTTP 400 Bad Request)
 * ==============================================================================================
 * 
 * WHY THIS IS USED (Enterprise Standard):
 * 1. Business Rule Violations: Thrown when an action violates domain business rules
 *    (e.g., assigning a ticket to a user who is not a Support Agent, or invalid status transition).
 * 2. Clear API Feedback: Returns descriptive messages to clients indicating why the operation failed.
 */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }
}
