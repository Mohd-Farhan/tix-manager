package com.support.strategy.routing;

import com.support.entity.Ticket;
import com.support.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ==============================================================================================
 * CONCRETE STRATEGY: RoundRobinRoutingStrategy (Fair Circular Distribution)
 * ==============================================================================================
 *
 * HOW THIS STRATEGY WORKS:
 * 1. Purpose: Distributes tickets strictly and sequentially across the agent pool
 *    (Agent 0 -> Agent 1 -> Agent 2 -> Agent 0 -> ...).
 *
 * 2. Concurrency & Thread-Safety:
 *    - In a high-traffic Spring Boot application, multiple requests arrive concurrently on different threads.
 *    - A standard `int counter++` suffers from race conditions (non-atomic read-modify-write).
 *    - We use `AtomicInteger` with `getAndIncrement()` to guarantee atomic, lock-free thread safety.
 *    - Modulo arithmetic with bitwise mask `& Integer.MAX_VALUE` prevents negative indices
 *      when the 32-bit counter eventually overflows.
 *
 * 3. Enterprise Value:
 *    - Delivers mathematically fair allocation without requiring database count queries on every assignment.
 */
@Slf4j
@Component
public class RoundRobinRoutingStrategy implements TicketRoutingStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public RoutingStrategyType getStrategyType() {
        return RoutingStrategyType.ROUND_ROBIN;
    }

    @Override
    public Optional<User> selectAgent(Ticket ticket, List<User> availableAgents) {
        if (availableAgents == null || availableAgents.isEmpty()) {
            log.warn("RoundRobinRouting: No available agents provided for routing.");
            return Optional.empty();
        }

        // Bitwise AND with Integer.MAX_VALUE ensures a non-negative index even on 32-bit integer overflow
        int index = (counter.getAndIncrement() & Integer.MAX_VALUE) % availableAgents.size();
        User selectedAgent = availableAgents.get(index);

        log.info("RoundRobinRouting: Selected agent '{}' (index={}/total={}) for ticket id={}",
                selectedAgent.getUsername(), index, availableAgents.size(), ticket.getId());

        return Optional.of(selectedAgent);
    }
}
