package com.support.state;

import com.support.entity.Ticket;
import com.support.entity.TicketStatus;
import com.support.entity.User;
import com.support.exception.InvalidOperationException;
import com.support.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * ==============================================================================================
 * CONCRETE STATE: ResolvedTicketState (Terminal & Reopening Management)
 * ==============================================================================================
 *
 * HOW THIS STATE WORKS:
 * 1. Semantics: Represents a completed ticket where remediation was delivered.
 *    The SLA clock is frozen because `resolvedAt` timestamp was captured.
 *
 * 2. Allowed Transitions:
 *    - RESOLVED -> IN_PROGRESS: VALID (Ticket Reopening).
 *      * If a customer replies that the fix did not work, the ticket reopens.
 *      * SIDE EFFECT: Clears `resolvedAt` (ticket is active again).
 *      * SIDE EFFECT: Unfreezes SLA calculation — if current time already passed `slaDueAt`,
 *        the ticket becomes marked as breached.
 *    - RESOLVED -> OPEN: INVALID (Enterprise business rule: a reopened ticket requires immediate
 *      active attention and cannot be thrown back into unassigned triage without an agent).
 *
 * 3. Assignment Behavior:
 *    - Assigning an agent directly to a RESOLVED ticket is disallowed.
 *      The ticket must explicitly be reopened first to prevent silent modifications to closed tickets.
 */
@Component
public class ResolvedTicketState implements TicketState {

    @Override
    public TicketStatus getStatus() {
        return TicketStatus.RESOLVED;
    }

    /**
     * Determines whether the ticket can transition to the given target status.
     *
     * Permitted:
     * - IN_PROGRESS: Reopening the ticket for further remediation.
     *
     * Forbidden:
     * - OPEN: Reopened tickets must be worked immediately (cannot jump back to unassigned OPEN).
     */
    @Override
    public boolean canTransitionTo(TicketStatus targetStatus) {
        return targetStatus == TicketStatus.IN_PROGRESS;
    }

    /**
     * Executes the transition from RESOLVED to targetStatus (Reopening).
     *
     * When reopening to IN_PROGRESS:
     * - Clears `resolvedAt` so the ticket is no longer considered closed.
     * - Evaluates whether current time has surpassed the original `slaDueAt` target.
     */
    @Override
    public void transitionTo(Ticket ticket, TicketStatus targetStatus, User changedBy) {
        if (!canTransitionTo(targetStatus)) {
            throw new InvalidStateTransitionException(
                    TicketStatus.RESOLVED,
                    targetStatus,
                    ticket.getId(),
                    "Tickets in RESOLVED state can only be reopened to IN_PROGRESS. Direct transition to OPEN is not allowed."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        // Enterprise Reopening Rule: Reset resolution timestamp
        ticket.setResolvedAt(null);

        // Enterprise Reopening Rule: Recalculate SLA breach against current time
        if (ticket.getSlaDueAt() != null) {
            ticket.setSlaBreached(now.isAfter(ticket.getSlaDueAt()));
        }

        ticket.setStatus(TicketStatus.IN_PROGRESS);
    }

    /**
     * Direct assignment to a RESOLVED ticket is forbidden.
     * Enforces the workflow: Reopen first -> then reassign.
     */
    @Override
    public void assign(Ticket ticket, User agent) {
        throw new InvalidOperationException(
                "Cannot assign an agent to a RESOLVED ticket. The ticket must be reopened to IN_PROGRESS first."
        );
    }
}
