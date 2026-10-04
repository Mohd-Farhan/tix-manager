package com.support.state;

import com.support.entity.Ticket;
import com.support.entity.TicketStatus;
import com.support.entity.User;
import com.support.exception.InvalidStateTransitionException;

/**
 * ==============================================================================================
 * GOF BEHAVIORAL PATTERN: STATE PATTERN (State Interface)
 * ==============================================================================================
 *
 * 1. WHAT IS THE STATE PATTERN?
 * ----------------------------------------------------------------------------------------------
 * The State Pattern is a Gang-of-Four (GoF) behavioral design pattern that allows an object
 * to alter its behavior when its internal state changes. The object will appear to change its class.
 *
 * In classic object-oriented architecture:
 * - CONTEXT: The entity holding the state (in our system, `com.support.entity.Ticket`).
 * - STATE INTERFACE: Defines methods for all state-specific behaviors (`TicketState`).
 * - CONCRETE STATES: Classes implementing behavior specific to each status
 *   (`OpenTicketState`, `InProgressTicketState`, `ResolvedTicketState`).
 *
 * 2. WHY USE THE STATE PATTERN INSTEAD OF IF/ELSE OR SWITCH LADDERS?
 * ----------------------------------------------------------------------------------------------
 * A naive implementation manages status transitions like this:
 *
 *   if (ticket.getStatus() == OPEN && newStatus == RESOLVED) {
 *       throw new Exception(...);
 *   } else if (ticket.getStatus() == RESOLVED && newStatus == OPEN) {
 *       // ...
 *   } else if (...) { ... }
 *
 * Pitfalls of procedural status checks:
 *  a. High Cyclomatic Complexity: As statuses grow (e.g., PENDING_CUSTOMER, CLOSED, ESCALATED),
 *     the nested conditional logic balloons into unmaintainable spaghetti code.
 *  b. Violates Single Responsibility Principle (SRP): One large service class ends up knowing
 *     the business rules, validation, SLA formulas, and side-effects of EVERY single state.
 *  c. Violates Open-Closed Principle (OCP): Adding a new state requires modifying every existing
 *     if/else block, creating a high risk of regression bugs in working workflows.
 *
 * With the State Pattern:
 *  - Each state class is self-contained and solely responsible for its own transition matrix.
 *  - Adding a new status (e.g. CLOSED) simply means creating `ClosedTicketState` without
 *    touching or risking the logic in `OpenTicketState` or `InProgressTicketState`.
 *
 * 3. HOW A STATE TRANSITION FLOWS IN THIS APPLICATION:
 * ----------------------------------------------------------------------------------------------
 *  [REST Request] -> [TicketController]
 *                         |
 *                         v
 *                 [TicketService]
 *                         |
 *                         v
 *              [TicketStateFactory] (finds the current state bean)
 *                         |
 *                         v
 *               [CurrentState.transitionTo(ticket, targetStatus, changedBy)]
 *                         |
 *        +----------------+----------------+
 *        | (Valid)                         | (Invalid)
 *        v                                 v
 *   Update status,                   Throw InvalidStateTransitionException
 *   Apply SLA rules/timestamps,       (Handled as HTTP 400 Bad Request)
 *   Save & Audit
 * ==============================================================================================
 */
public interface TicketState {

    /**
     * Identifies the enum status value corresponding to this state object.
     *
     * @return The TicketStatus enum managed by this concrete state.
     */
    TicketStatus getStatus();

    /**
     * Evaluates whether a transition from this current state to the specified target state is permitted.
     *
     * Pure query method (predicate) — causes no side effects.
     *
     * @param targetStatus The requested destination status.
     * @return true if the transition obeys enterprise business rules; false otherwise.
     */
    boolean canTransitionTo(TicketStatus targetStatus);

    /**
     * Executes the state transition, including state-specific business side effects
     * (e.g., SLA deadline freezing, timestamp setting, reopening SLA recalculation).
     *
     * @param ticket       The Ticket entity being transitioned (acts as the Context).
     * @param targetStatus The requested destination status.
     * @param changedBy    The User requesting this transition (for audit and authorization).
     * @throws InvalidStateTransitionException if canTransitionTo(targetStatus) returns false.
     */
    void transitionTo(Ticket ticket, TicketStatus targetStatus, User changedBy);

    /**
     * Executes agent assignment logic specific to this state.
     *
     * For example:
     * - An OPEN ticket automatically transitions to IN_PROGRESS upon agent assignment.
     * - An IN_PROGRESS ticket simply updates the assigned agent reference.
     * - A RESOLVED ticket rejects assignment until it is formally reopened.
     *
     * @param ticket The Ticket entity being assigned.
     * @param agent  The User (support agent) being assigned.
     */
    void assign(Ticket ticket, User agent);
}
