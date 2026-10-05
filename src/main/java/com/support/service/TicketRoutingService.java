package com.support.service;

import com.support.entity.Ticket;
import com.support.entity.User;
import com.support.entity.UserRole;
import com.support.exception.InvalidOperationException;
import com.support.repository.UserRepository;
import com.support.strategy.routing.RoutingStrategyType;
import com.support.strategy.routing.TicketRoutingContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ==============================================================================================
 * SERVICE: TicketRoutingService (Routing Orchestration)
 * ==============================================================================================
 *
 * HOW THIS SERVICE WORKS:
 * 1. Purpose: Bridges the persistence layer (finding eligible agents in the DB) with the
 *    Strategy Pattern layer (executing the selected routing algorithm).
 *
 * 2. Separation of Concerns (SoC):
 *    - The Strategy classes (RoundRobin, WorkloadBalanced, PriorityBased) are pure algorithms.
 *      They do NOT query for user lists or throw HTTP-mapped domain exceptions.
 *    - This Service is responsible for:
 *      * Fetching the active agent roster from UserRepository.
 *      * Filtering out deleted or unavailable users.
 *      * Delegating to `TicketRoutingContext` to run the algorithm.
 *      * Raising domain exceptions (e.g. 400 Bad Request when 0 agents exist) with clear error messages.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class TicketRoutingService {

    private final UserRepository userRepository;
    private final TicketRoutingContext routingContext;

    @Autowired
    public TicketRoutingService(UserRepository userRepository, TicketRoutingContext routingContext) {
        this.userRepository = userRepository;
        this.routingContext = routingContext;
    }

    /**
     * Resolves the most suitable agent for the given ticket based on the requested strategy.
     *
     * @param ticket       The Ticket awaiting assignment.
     * @param strategyType The routing strategy to apply (null defaults to WORKLOAD_BALANCED).
     * @return The selected User (support agent).
     * @throws InvalidOperationException if no active agents are available in the system.
     */
    public User resolveAgent(Ticket ticket, RoutingStrategyType strategyType) {
        // 1. Fetch all support agents from the database
        List<User> activeAgents = userRepository.findByRole(UserRole.SUPPORT_AGENT)
                .stream()
                .filter(agent -> !agent.isDeleted())
                .toList();

        if (activeAgents.isEmpty()) {
            log.error("Ticket routing failed: No active SUPPORT_AGENT users found in the system.");
            throw new InvalidOperationException("No active support agents available for automated routing.");
        }

        // 2. Delegate to the Strategy Pattern context to pick an agent
        return routingContext.routeTicket(ticket, activeAgents, strategyType)
                .orElseThrow(() -> new InvalidOperationException("Routing strategy could not select an agent."));
    }
}
