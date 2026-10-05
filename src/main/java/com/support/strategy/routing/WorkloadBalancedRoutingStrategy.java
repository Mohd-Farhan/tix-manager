package com.support.strategy.routing;

import com.support.entity.Ticket;
import com.support.entity.User;
import com.support.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * ==============================================================================================
 * CONCRETE STRATEGY: WorkloadBalancedRoutingStrategy (Least-Busy Load Balancing)
 * ==============================================================================================
 *
 * HOW THIS STRATEGY WORKS:
 * 1. Purpose: Ensures equitable distribution of open workloads across the team.
 *    No single support agent is overwhelmed while others sit idle.
 *
 * 2. Algorithm:
 *    - Queries the database for the active ticket count (OPEN + IN_PROGRESS) of each candidate agent.
 *    - Selects the agent with the lowest active count.
 *    - In case of a tie (e.g., two agents each have 2 tickets), it breaks the tie deterministically
 *      using the lowest user ID to avoid non-deterministic behavior across clusters.
 *
 * 3. Enterprise Value:
 *    - Reduces mean time to resolution (MTTR) by routing new work to agents with immediate capacity.
 */
@Slf4j
@Component
public class WorkloadBalancedRoutingStrategy implements TicketRoutingStrategy {

    private final TicketRepository ticketRepository;

    @Autowired
    public WorkloadBalancedRoutingStrategy(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    public RoutingStrategyType getStrategyType() {
        return RoutingStrategyType.WORKLOAD_BALANCED;
    }

    @Override
    public Optional<User> selectAgent(Ticket ticket, List<User> availableAgents) {
        if (availableAgents == null || availableAgents.isEmpty()) {
            log.warn("WorkloadBalancedRouting: No available agents provided for routing.");
            return Optional.empty();
        }

        // Find agent with the minimum active ticket workload
        User leastBusyAgent = availableAgents.stream()
                .min(Comparator
                        .comparingLong((User agent) -> ticketRepository.countActiveTicketsByAgentId(agent.getId()))
                        .thenComparing(User::getId)) // Deterministic tie-breaker
                .orElse(null);

        if (leastBusyAgent != null) {
            long activeCount = ticketRepository.countActiveTicketsByAgentId(leastBusyAgent.getId());
            log.info("WorkloadBalancedRouting: Selected agent '{}' (id={}) with {} active tickets for ticket id={}",
                    leastBusyAgent.getUsername(), leastBusyAgent.getId(), activeCount, ticket.getId());
        }

        return Optional.ofNullable(leastBusyAgent);
    }
}
