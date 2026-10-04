package com.support.state;

import com.support.entity.Ticket;
import com.support.entity.TicketStatus;
import com.support.entity.User;
import com.support.exception.InvalidStateTransitionException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * ==============================================================================================
 * CONCRETE STATE: InProgressTicketState (Active Investigation & Remediation)
 * ==============================================================================================
 *
 * HOW THIS STATE WORKS:
 * 1. Semantics: Represents a ticket currently under active investigation or work by an agent.
 *    The SLA clock is actively ticking against its target resolution deadline (`slaDueAt`).
 *
 * 2. Allowed Transitions:
 *    - IN_PROGRESS -> RESOLVED: VALID (Work completed, issue fixed).
 *      * SIDE EFFECT: Freezes resolution timestamp (`resolvedAt = now`).
 *      * SIDE EFFECT: Evaluates whether resolution occurred before or after SLA target deadline.
 *    - IN_PROGRESS -> OPEN: VALID (Ticket unassigned or returned to pool for re-triage).
 *
 * 3. Assignment Behavior:
 *    - Reassigns the ticket to a new agent while maintaining the IN_PROGRESS status.
 */
@Component
public class InProgressTicketState implements TicketState {

    @Override
    public TicketStatus getStatus() {
        return TicketStatus.IN_PROGRESS;
    }

    /**
     * Determines whether the ticket can transition to the given target status.
     *
     * Permitted:
     * - RESOLVED: Remediation complete.
     * - OPEN: Return ticket to triage queue.
     */
    @Override
    public boolean canTransitionTo(TicketStatus targetStatus) {
        return targetStatus == TicketStatus.RESOLVED || targetStatus == TicketStatus.OPEN;
    }

    /**
     * Executes the transition from IN_PROGRESS to targetStatus.
     *
     * When resolving:
     * - Freezes `resolvedAt` timestamp.
     * - Checks if resolution happened past `slaDueAt` to flag `slaBreached`.
     */
    @Override
    public void transitionTo(Ticket ticket, TicketStatus targetStatus, User changedBy) {
        if (!canTransitionTo(targetStatus)) {
            throw new InvalidStateTransitionException(
                    TicketStatus.IN_PROGRESS,
                    targetStatus,
                    ticket.getId(),
                    "Tickets in IN_PROGRESS state can only transition to RESOLVED (completion) or OPEN (returned to queue)."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        if (targetStatus == TicketStatus.RESOLVED) {
            // Enterprise SLA Rule: Freeze resolution timestamp
            ticket.setResolvedAt(now);

            // Enterprise SLA Rule: Mark breach if current time exceeded the SLA target
            if (ticket.getSlaDueAt() != null && now.isAfter(ticket.getSlaDueAt())) {
                ticket.setSlaBreached(true);
            }
        }

        ticket.setStatus(targetStatus);
    }

    /**
     * Reassigning an in-progress ticket updates the responsible agent
     * and keeps the ticket in IN_PROGRESS.
     */
    @Override
    public void assign(Ticket ticket, User agent) {
        ticket.setAssignedAgent(agent);
        // Status remains IN_PROGRESS
    }
}
