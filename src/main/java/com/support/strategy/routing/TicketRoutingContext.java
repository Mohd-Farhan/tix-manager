package com.support.strategy.routing;

import com.support.entity.Ticket;
import com.support.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * ==============================================================================================
 * GOF BEHAVIORAL PATTERN: STRATEGY PATTERN (Context & Registry)
 * ==============================================================================================
 *
 * 1. WHAT ROLE DOES THIS CLASS PLAY?
 * ----------------------------------------------------------------------------------------------
 * In the GoF Strategy Pattern, the Context maintains a reference to a Strategy object and
 * delegates the algorithm execution to it.
 *
 * Here, `TicketRoutingContext`:
 * - Acts as both the Context and the Strategy Registry.
 * - Collects all registered `TicketRoutingStrategy` implementations from the Spring container.
 * - Dynamically retrieves the requested strategy at runtime based on `RoutingStrategyType`.
 * - Executes the algorithm without coupling the client (services or controllers) to concrete classes.
 *
 * 2. SPRING INJECTION & OPEN-CLOSED PRINCIPLE (OCP):
 * ----------------------------------------------------------------------------------------------
 * Notice the constructor: `@Autowired public TicketRoutingContext(List<TicketRoutingStrategy> strategies)`
 * Spring automatically finds every bean that implements `TicketRoutingStrategy` and puts it into this list.
 *
 * When you add a new strategy in Phase 3 (e.g. `AiAgentRoutingStrategy`):
 * - You create the new class annotated with `@Component`.
 * - Spring automatically injects it here.
 * - YOU DO NOT HAVE TO CHANGE A SINGLE LINE IN THIS CONTEXT OR SERVICE!
 */
@Component
public class TicketRoutingContext {

    private final Map<RoutingStrategyType, TicketRoutingStrategy> strategyMap = new EnumMap<>(RoutingStrategyType.class);

    /**
     * Spring-managed constructor: Injects all discovered strategy beans.
     */
    @Autowired
    public TicketRoutingContext(List<TicketRoutingStrategy> strategies) {
        for (TicketRoutingStrategy strategy : strategies) {
            strategyMap.put(strategy.getStrategyType(), strategy);
        }
    }

    /**
     * Fallback constructor for fast unit tests without full Spring bootstrap.
     */
    public TicketRoutingContext(TicketRoutingStrategy... strategies) {
        for (TicketRoutingStrategy strategy : strategies) {
            strategyMap.put(strategy.getStrategyType(), strategy);
        }
    }

    /**
     * Retrieves the concrete routing strategy corresponding to the requested type.
     * Defaults to WORKLOAD_BALANCED if strategyType is null.
     *
     * @param strategyType The requested routing algorithm.
     * @return The matching TicketRoutingStrategy instance.
     * @throws IllegalArgumentException if no strategy is registered for the given type.
     */
    public TicketRoutingStrategy getStrategy(RoutingStrategyType strategyType) {
        if (strategyType == null) {
            strategyType = RoutingStrategyType.WORKLOAD_BALANCED;
        }

        TicketRoutingStrategy strategy = strategyMap.get(strategyType);
        if (strategy == null) {
            throw new IllegalArgumentException("No routing strategy registered for type: " + strategyType);
        }
        return strategy;
    }

    /**
     * Delegates ticket routing execution to the selected strategy.
     *
     * @param ticket          The Ticket to be assigned.
     * @param availableAgents The candidate support agents.
     * @param strategyType    The chosen algorithm.
     * @return Optional containing the selected agent, or empty if none could be picked.
     */
    public Optional<User> routeTicket(Ticket ticket, List<User> availableAgents, RoutingStrategyType strategyType) {
        TicketRoutingStrategy strategy = getStrategy(strategyType);
        return strategy.selectAgent(ticket, availableAgents);
    }
}
