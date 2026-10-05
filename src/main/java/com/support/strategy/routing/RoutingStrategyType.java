package com.support.strategy.routing;

/**
 * ==============================================================================================
 * ENUM: RoutingStrategyType
 * ==============================================================================================
 *
 * DESIGN PATTERN ROLE:
 * Represents the available routing algorithms (strategies) in the Strategy Pattern.
 * Used by controllers, services, and configuration to select which algorithm to apply.
 */
public enum RoutingStrategyType {

    /**
     * WORKLOAD_BALANCED:
     * Assigns the ticket to the support agent who currently has the fewest active
     * (OPEN or IN_PROGRESS) tickets. Prevents bottlenecks and balances team workload.
     */
    WORKLOAD_BALANCED,

    /**
     * ROUND_ROBIN:
     * Assigns tickets sequentially in circular order (Agent A -> Agent B -> Agent C -> Agent A).
     * Ensures strict mathematical fairness of ticket distribution regardless of resolution time.
     */
    ROUND_ROBIN,

    /**
     * PRIORITY_BASED:
     * SLA-driven routing. HIGH-priority tickets are routed strictly to agents with the lightest
     * workload for immediate remediation, while MEDIUM and LOW tickets are distributed evenly.
     */
    PRIORITY_BASED
}
