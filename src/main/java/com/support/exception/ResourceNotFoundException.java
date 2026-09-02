package com.support.exception;

/**
 * ==============================================================================================
 * DOMAIN EXCEPTION: ResourceNotFoundException (HTTP 404)
 * ==============================================================================================
 * 
 * WHY THIS IS USED (Enterprise Standard):
 * 1. Semantic Clarity: Signals that a requested domain entity (Ticket, User, Message) does not exist.
 * 2. Decoupling: Decouples business logic from persistence framework specifics
 *    (e.g., jakarta.persistence.EntityNotFoundException).
 * 3. Automatic 404 Mapping: Handled by GlobalExceptionHandler to return standard RFC JSON 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
