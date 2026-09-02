package com.support.exception;

/**
 * ==============================================================================================
 * DOMAIN EXCEPTION: DuplicateResourceException (HTTP 409 Conflict)
 * ==============================================================================================
 * 
 * WHY THIS IS USED (Enterprise Standard):
 * 1. Conflict Status (409): HTTP 409 Conflict is the RFC-standard status code when a client
 *    attempts to create a resource that violates a unique constraint (e.g. duplicate username/email).
 * 2. Targeted Frontend Handling: Allows Angular frontend to specifically highlight the duplicate
 *    field on registration forms rather than displaying a generic 500 error.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

    public DuplicateResourceException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s with %s '%s' already exists.", resourceName, fieldName, fieldValue));
    }
}
