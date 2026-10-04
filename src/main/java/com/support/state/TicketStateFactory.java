package com.support.state;

import com.support.entity.Ticket;
import com.support.entity.TicketStatus;
import com.support.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * ==============================================================================================
 * STATE COORDINATOR: TicketStateFactory (Registry & Transition Coordinator)
 * ==============================================================================================
 *
 * 1. WHAT ROLE DOES THIS CLASS PLAY?
 * ----------------------------------------------------------------------------------------------
 * In Spring applications, state objects are typically stateless singletons managed by the
 * Spring application context (registered as `@Component` beans).
 *
 * However, the database entity `Ticket` is a stateful JPA entity that stores `status` as an enum.
 * `TicketStateFactory` acts as the bridge (Registry / Coordinator):
 * - It maintains an internal map of `TicketStatus -> TicketState`.
 * - Given a ticket's current enum status, it fetches the corresponding polymorphic state instance.
 * - It orchestrates transitions and assignments cleanly without exposing state instantiation
 *   to service or controller callers.
 *
 * 2. SPRING INJECTION MECHANISM:
 * ----------------------------------------------------------------------------------------------
 * Spring's `@Autowired` on `List<TicketState>` automatically scans all classes implementing
 * `TicketState` (OpenTicketState, InProgressTicketState, ResolvedTicketState) and injects them.
 * This adheres strictly to the Open-Closed Principle (OCP):
 * When you add a new state bean in the future (e.g., `EscalatedTicketState`), Spring automatically
 * discovers and registers it here without changing ANY code in this factory!
 *
 * 3. UNIT TEST FRIENDLINESS:
 * ----------------------------------------------------------------------------------------------
 * A default constructor is also provided with default state instances, allowing unit tests
 * to instantiate `new TicketStateFactory()` directly without needing a full Spring context.
 */
@Component
public class TicketStateFactory {

    private final Map<TicketStatus, TicketState> stateMap = new EnumMap<>(TicketStatus.class);

    /**
     * Spring-managed constructor: Injects all discovered TicketState beans.
     */
    @Autowired
    public TicketStateFactory(List<TicketState> states) {
        for (TicketState state : states) {
            stateMap.put(state.getStatus(), state);
        }
    }

    /**
     * Fallback zero-arg constructor for fast standalone unit tests.
     */
    public TicketStateFactory() {
        this(List.of(
                new OpenTicketState(),
                new InProgressTicketState(),
                new ResolvedTicketState()
        ));
    }

    /**
     * Retrieves the concrete state object corresponding to a TicketStatus enum.
     *
     * @param status The TicketStatus enum value.
     * @return The matching TicketState instance.
     * @throws IllegalArgumentException if no state handler is registered for the status.
     */
    public TicketState getState(TicketStatus status) {
        if (status == null) {
            status = TicketStatus.OPEN;
        }
        TicketState state = stateMap.get(status);
        if (state == null) {
            throw new IllegalArgumentException("No TicketState registered for status: " + status);
        }
        return state;
    }

    /**
     * Checks if a transition from current status to target status is valid.
     * Useful for UI pre-checks (e.g. disabling invalid buttons) and API guards.
     */
    public boolean canTransition(TicketStatus currentStatus, TicketStatus targetStatus) {
        if (currentStatus == targetStatus) {
            return true; // No-op transition is always permissible
        }
        return getState(currentStatus).canTransitionTo(targetStatus);
    }

    /**
     * Coordinates a state transition on a ticket.
     *
     * Steps executed:
     * 1. Check for no-op (ticket already has the requested status).
     * 2. Look up the current state handler for ticket.getStatus().
     * 3. Delegate execution and side-effects to currentState.transitionTo(...).
     *
     * @param ticket       The Ticket entity being updated.
     * @param newStatus    The requested new status.
     * @param changedBy    The User requesting the change.
     */
    public void applyTransition(Ticket ticket, TicketStatus newStatus, User changedBy) {
        TicketStatus currentStatus = ticket.getStatus();
        if (currentStatus == newStatus) {
            // Idempotent: nothing changes if target matches current
            return;
        }

        TicketState currentState = getState(currentStatus);
        currentState.transitionTo(ticket, newStatus, changedBy);
    }

    /**
     * Coordinates agent assignment on a ticket.
     *
     * Steps executed:
     * 1. Look up the current state handler for ticket.getStatus().
     * 2. Delegate assignment and state adjustments to currentState.assign(...).
     *
     * @param ticket The Ticket entity being assigned.
     * @param agent  The User (support agent) being assigned.
     */
    public void applyAssignment(Ticket ticket, User agent) {
        TicketState currentState = getState(ticket.getStatus());
        currentState.assign(ticket, agent);
    }
}
