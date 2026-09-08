package com.support.config;

import com.support.dto.ErrorResponse;
import com.support.exception.DuplicateResourceException;
import com.support.exception.InvalidOperationException;
import com.support.exception.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ==============================================================================================
 * GLOBAL REST EXCEPTION HANDLER (@RestControllerAdvice)
 * ==============================================================================================
 * 
 * WHY THIS IS USED (Enterprise Standard):
 * 1. Uniform RFC 7807 Error Responses: Guarantees that EVERY endpoint across the entire application
 *    returns an identical, predictable JSON schema whenever an error occurs. The Angular frontend
 *    can safely bind to `error.status`, `error.message`, and `error.fieldErrors`.
 * 2. Information Hiding & Security: Prevents raw Java stack traces, database schema details,
 *    and SQL syntax from leaking to external clients in HTTP 500 scenarios.
 * 3. HTTP Semantic Correctness: Maps application exceptions to precise HTTP status codes:
 *    - 400 (Bad Request): Validation failure, malformed JSON, invalid business operations.
 *    - 401 (Unauthorized): Bad credentials or expired/missing tokens.
 *    - 403 (Forbidden): Insufficient RBAC roles or attempting to access another user's ticket.
 *    - 404 (Not Found): Requested user, ticket, or message ID does not exist.
 *    - 405 (Method Not Allowed): Attempting POST on a GET-only endpoint.
 *    - 409 (Conflict): Duplicate unique constraints (e.g. username/email already registered).
 *    - 500 (Internal Server Error): Unhandled unexpected system failures.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 1. HANDLER: Custom Domain ResourceNotFoundException & JPA EntityNotFoundException
     * HTTP STATUS: 404 Not Found
     * WHY: Signals that a requested ID (e.g. /api/tickets/999) does not exist in the database.
     */
    @ExceptionHandler({ResourceNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleResourceNotFound(RuntimeException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    /**
     * 2. HANDLER: DuplicateResourceException
     * HTTP STATUS: 409 Conflict
     * WHY: Standard RFC code when creating a resource that violates a unique constraint
     *      (e.g., registering an account with an email/username that is already taken).
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * 2b. HANDLER: ObjectOptimisticLockingFailureException & OptimisticLockException
     * HTTP STATUS: 409 Conflict
     * WHY (Concurrency & Race Condition Prevention): 
     *      Triggered when two concurrent transactions try to modify the same ticket simultaneously.
     *      Rather than overwriting the previous update ("lost update" anomaly), Hibernate's @Version
     *      check aborts the second update and notifies the client to refresh.
     */
    @ExceptionHandler({
            org.springframework.orm.ObjectOptimisticLockingFailureException.class,
            jakarta.persistence.OptimisticLockException.class
    })
    public ResponseEntity<ErrorResponse> handleOptimisticLockingFailure(Exception ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error("Concurrent Modification Conflict")
                .message("This ticket was updated by another user or agent in the background. Please refresh and try again.")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /**
     * 3. HANDLER: InvalidOperationException & IllegalArgumentException
     * HTTP STATUS: 400 Bad Request
     * WHY: Triggered when a requested business operation is illegal
     *      (e.g., assigning a ticket to a customer instead of a support agent).
     */
    @ExceptionHandler({InvalidOperationException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleInvalidOperation(RuntimeException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 4. HANDLER: BadCredentialsException
     * HTTP STATUS: 401 Unauthorized
     * WHY: Triggered during authentication when the password hash or username does not match.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Invalid username or password")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * 5. HANDLER: AccessDeniedException (Spring Security)
     * HTTP STATUS: 403 Forbidden
     * WHY: Triggered when an authenticated user lacks the required role or does not own the resource
     *      (e.g. customer attempting to access /admin/dashboard or another customer's ticket).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message("Access Denied: You do not have permission to perform this action or view this resource.")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * 6. HANDLER: MethodArgumentNotValidException (Spring Bean Validation @Valid)
     * HTTP STATUS: 400 Bad Request
     * WHY: Collects and maps all field-level validation errors (@NotBlank, @Size, @Email, @NotNull)
     *      into a clean array of { field, message } pairs so frontend forms can display inline errors.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> new ErrorResponse.FieldErrorDetail(err.getField(), err.getDefaultMessage()))
                .collect(Collectors.toList());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("The request payload contains one or more validation errors.")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 7. HANDLER: HttpMessageNotReadableException
     * HTTP STATUS: 400 Bad Request
     * WHY: Triggered when the client sends malformed JSON, unparseable dates, or invalid enum values.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Malformed JSON Request")
                .message("Request body could not be parsed. Verify JSON syntax and enum values.")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    /**
     * 8. HANDLER: HttpRequestMethodNotSupportedException
     * HTTP STATUS: 405 Method Not Allowed
     * WHY: Triggered when an unsupported HTTP method is invoked (e.g. POST on a GET-only route).
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error(HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase())
                .message(String.format("HTTP method '%s' is not supported for this endpoint. Supported methods: %s",
                        ex.getMethod(), String.join(", ", ex.getSupportedMethods() != null ? ex.getSupportedMethods() : new String[0])))
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(error);
    }

    /**
     * 9. HANDLER: Unhandled Generic Exception
     * HTTP STATUS: 500 Internal Server Error
     * WHY: Catch-all safety net. Logs the error and returns a sanitized generic message to clients.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected internal server error occurred. Please try again later.")
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
