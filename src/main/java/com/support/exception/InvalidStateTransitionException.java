package com.support.exception;

import com.support.entity.TicketStatus;
import lombok.Getter;

/**
 * ==============================================================================================
 * DOMAIN EXCEPTION: InvalidStateTransitionException (HTTP 400 Bad Request)
 * ==============================================================================================
 *
 * DESIGN PATTERN CONTEXT:
 * In the GoF State Pattern, state transitions are governed by strict business invariants.
 * When a client or service attempts an illegal state transition (e.g., trying to jump
 * directly from OPEN to RESOLVED without active investigation), this domain exception is thrown.
 *
 * WHY EXTEND InvalidOperationException?
 * 1. Global Exception Mapping: InvalidOperationException is already registered in
 *    GlobalExceptionHandler to produce RFC-7807 compliant HTTP 400 Bad Request responses.
 * 2. Specialized Diagnostics: Keeps strongly-typed domain metadata (fromStatus, toStatus, ticketId)
 *    so audit logs, telemetry, and client error payloads pinpoint the exact lifecycle violation.
 */
@Getter
public class InvalidStateTransitionException extends InvalidOperationException {

    private final TicketStatus fromStatus;
    private final TicketStatus toStatus;
    private final Long ticketId;

    public InvalidStateTransitionException(TicketStatus fromStatus, TicketStatus toStatus, Long ticketId) {
        super(String.format("Invalid state transition from '%s' to '%s' for ticket id=%d. " +
                "Tickets must follow valid lifecycle progression.",
                fromStatus, toStatus, ticketId));
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.ticketId = ticketId;
    }

    public InvalidStateTransitionException(TicketStatus fromStatus, TicketStatus toStatus, Long ticketId, String explanation) {
        super(String.format("Invalid state transition from '%s' to '%s' for ticket id=%d: %s",
                fromStatus, toStatus, ticketId, explanation));
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.ticketId = ticketId;
    }
}
