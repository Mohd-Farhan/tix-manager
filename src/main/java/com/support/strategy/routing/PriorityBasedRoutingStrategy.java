package com.support.strategy.routing;

import com.support.entity.Ticket;
import com.support.entity.TicketPriority;
import com.support.entity.User;
import com.support.repository.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ==============================================================================================
 * CONCRETE STRATEGY: PriorityBasedRoutingStrategy (SLA & Urgency-Weighted)
 * ==============================================================================================
 *
 * HOW THIS STRATEGY WORKS:
 * 1. Purpose: Prioritizes critical/high-urgency tickets by routing them to the agent with the
 *    greatest immediate bandwidth (lowest active workload), preventing SLA breaches.
 *    For lower-priority tickets (LOW / MEDIUM), it uses circular fair rotation so that
 *    low-priority tasks are distributed evenly across the team.
 *
 * 2. Enterprise Rule:
 *    - HIGH Priority: Assign to agent with minimum active tickets (triage fast lane).
 *    - MEDIUM / LOW Priority: Assign via circular round-robin distribution.
 *
 * 3. Enterprise Value:
 *    - Reduces SLA breach rates on high-impact customer issues while maintaining fair work balance.
 */
@Slf4j
@Component
public class PriorityBasedRoutingStrategy implements TicketRoutingStrategy {

    private final TicketRepository ticketRepository;
    private final AtomicInteger standardPriorityCounter = new AtomicInteger(0);

    @Autowired
    public PriorityBasedRoutingStrategy(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    public RoutingStrategyType getStrategyType() {
        return RoutingStrategyType.PRIORITY_BASED;
    }

    @Override
    public Optional<User> selectAgent(Ticket ticket, List<User> availableAgents) {
        if (availableAgents == null || availableAgents.isEmpty()) {
            log.warn("PriorityBasedRouting: No available agents provided for routing.");
            return Optional.empty();
        }

        TicketPriority priority = ticket.getPriority() != null ? ticket.getPriority() : TicketPriority.MEDIUM;

        if (priority == TicketPriority.HIGH) {
            // HIGH PRIORITY FAST-LANE: Direct to the agent with the lowest active workload
            User leastBusyAgent = availableAgents.stream()
                    .min(Comparator
                            .comparingLong((User agent) -> ticketRepository.countActiveTicketsByAgentId(agent.getId()))
                            .thenComparing(User::getId))
                    .orElse(null);

            log.info("PriorityBasedRouting: HIGH priority ticket id={} assigned to least busy agent '{}'",
                    ticket.getId(), leastBusyAgent != null ? leastBusyAgent.getUsername() : "null");
            return Optional.ofNullable(leastBusyAgent);
        }

        // STANDARD PRIORITY (MEDIUM / LOW): Distribute circularly to balance standard queue
        int index = (standardPriorityCounter.getAndIncrement() & Integer.MAX_VALUE) % availableAgents.size();
        User selectedAgent = availableAgents.get(index);

        log.info("PriorityBasedRouting: {} priority ticket id={} assigned circularly to agent '{}'",
                priority, ticket.getId(), selectedAgent.getUsername());
        return Optional.of(selectedAgent);
    }
}
