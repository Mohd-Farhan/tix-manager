package com.support.strategy.routing;

import com.support.entity.Ticket;
import com.support.entity.User;

import java.util.List;
import java.util.Optional;

/**
 * ==============================================================================================
 * GOF BEHAVIORAL PATTERN: STRATEGY PATTERN (Strategy Interface)
 * ==============================================================================================
 *
 * 1. WHAT IS THE STRATEGY PATTERN?
 * ----------------------------------------------------------------------------------------------
 * The Strategy Pattern is a Gang-of-Four (GoF) behavioral design pattern that defines
 * a family of interchangeable algorithms, encapsulates each algorithm into its own class,
 * and makes them completely interchangeable at runtime.
 *
 * Classic Strategy Pattern components:
 * - STRATEGY INTERFACE: Defines the contract that all routing algorithms must satisfy (`TicketRoutingStrategy`).
 * - CONCRETE STRATEGIES: Individual classes implementing specific algorithms
 *   (`WorkloadBalancedRoutingStrategy`, `RoundRobinRoutingStrategy`, `PriorityBasedRoutingStrategy`).
 * - CONTEXT: The class that holds a reference to a strategy and invokes it (`TicketRoutingContext` / `TicketRoutingService`).
 * - CLIENT: The API layer (`TicketController` / `TicketService`) which chooses or defaults a strategy.
 *
 * 2. WHY USE THE STRATEGY PATTERN INSTEAD OF A PROCEDURAL SWITCH/IF-ELSE?
 * ----------------------------------------------------------------------------------------------
 * Without the Strategy Pattern, routing looks like this:
 *
 *   if (strategy == "ROUND_ROBIN") {
 *       // 30 lines of round-robin calculation
 *   } else if (strategy == "WORKLOAD") {
 *       // 40 lines of DB workload queries and min-filtering
 *   } else if (strategy == "PRIORITY") {
 *       // 50 lines of priority matrix rules
 *   }
 *
 * Problems with procedural routing:
 *  a. Violates Open-Closed Principle (OCP):
 *     Every time you add a new algorithm (e.g. AI-driven routing in Phase 3!), you must open
 *     and edit the central service class, risking unintended regressions in existing algorithms.
 *  b. Violates Single Responsibility Principle (SRP):
 *     A single service ends up containing all business logic, mathematical algorithms, and state.
 *  c. Hard to Test:
 *     You cannot unit-test the Round-Robin math independently from database queries or JPA mocks.
 *
 * With the Strategy Pattern:
 *  - Each routing algorithm lives in its own focused, easily-testable class.
 *  - Adding an AI agent router in Phase 3 simply requires creating `AiAgentRoutingStrategy`
 *    implementing this interface without touching a single line of existing code.
 *
 * 3. HOW A ROUTING CALL FLOWS IN THIS SYSTEM:
 * ----------------------------------------------------------------------------------------------
 *  [REST: PUT /api/tickets/{id}/auto-assign?strategy=WORKLOAD_BALANCED]
 *                         │
 *                         ▼
 *               [TicketController]
 *                         │
 *                         ▼
 *           [TicketService.autoAssignTicket]
 *                         │
 *                         ▼
 *         [TicketRoutingService.resolveAgent] (retrieves active agent list)
 *                         │
 *                         ▼
 *        [TicketRoutingContext.getStrategy(type)] (selects concrete bean)
 *                         │
 *                         ▼
 *      [ChosenStrategy.selectAgent(ticket, agents)] (executes algorithm)
 *                         │
 *                         ▼
 *       Selected Agent returned -> State Pattern assigns ticket!
 * ==============================================================================================
 */
public interface TicketRoutingStrategy {

    /**
     * Identifies the strategy type handled by this implementation.
     *
     * @return The corresponding RoutingStrategyType enum value.
     */
    RoutingStrategyType getStrategyType();

    /**
     * Evaluates the ticket and list of available support agents to pick the best agent.
     *
     * @param ticket          The Ticket entity awaiting assignment (provides priority, title, etc.).
     * @param availableAgents The candidate support agents currently active and eligible for assignment.
     * @return An Optional containing the selected User (agent), or Optional.empty() if no agent could be chosen.
     */
    Optional<User> selectAgent(Ticket ticket, List<User> availableAgents);
}
