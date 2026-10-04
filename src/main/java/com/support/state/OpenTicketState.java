package com.support.state;

import com.support.entity.Ticket;
import com.support.entity.TicketStatus;
import com.support.entity.User;
import com.support.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

/**
 * ==============================================================================================
 * CONCRETE STATE: OpenTicketState (Lifecycle Initial State)
 * ==============================================================================================
 *
 * HOW THIS STATE WORKS:
 * 1. Semantics: Represents a freshly submitted customer ticket sitting in the triage pool.
 *    No work has officially started on it yet, and no resolution has been proposed.
 *
 * 2. Allowed Transitions:
 *    - OPEN -> IN_PROGRESS: VALID (When an agent claims the ticket or manual work begins).
 *    - OPEN -> RESOLVED: INVALID (Enterprise business rule: tickets cannot be resolved
 *      without triage/investigation. Jumping directly to RESOLVED skips quality assurance).
 *
 * 3. Assignment Behavior:
 *    - When an agent is assigned to an OPEN ticket, the ticket automatically transitions
 *      to IN_PROGRESS. This eliminates manual double-handling (assigning AND THEN changing status).
 */
@Component
public class OpenTicketState implements TicketState {

    @Override
    public TicketStatus getStatus() {
        return TicketStatus.OPEN;
    }

    /**
     * Determines whether the ticket can transition to the given target status.
     *
     * Permitted:
     * - IN_PROGRESS: Begins active handling.
     *
     * Forbidden:
     * - RESOLVED: Direct closure from OPEN is blocked by triage integrity rules.
     */
    @Override
    public boolean canTransitionTo(TicketStatus targetStatus) {
        return targetStatus == TicketStatus.IN_PROGRESS;
    }

    /**
     * Executes the transition from OPEN to targetStatus.
     *
     * If targetStatus is IN_PROGRESS, the status is updated.
     * Otherwise, an InvalidStateTransitionException is thrown.
     */
    @Override
    public void transitionTo(Ticket ticket, TicketStatus targetStatus, User changedBy) {
        if (!canTransitionTo(targetStatus)) {
            throw new InvalidStateTransitionException(
                    TicketStatus.OPEN,
                    targetStatus,
                    ticket.getId(),
                    "Tickets in OPEN state cannot be directly resolved. They must first transition to IN_PROGRESS for triage."
            );
        }

        // Apply state transition
        ticket.setStatus(targetStatus);
    }

    /**
     * Assigning an agent to an OPEN ticket starts active work,
     * so it automatically promotes the status to IN_PROGRESS.
     */
    @Override
    public void assign(Ticket ticket, User agent) {
        ticket.setAssignedAgent(agent);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
    }
}
