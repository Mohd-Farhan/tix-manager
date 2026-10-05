package com.support.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.support.entity.Ticket;
import com.support.entity.TicketPriority;
import com.support.entity.TicketStatus;

/**
 * ==============================================================================================
 * REPOSITORY: TicketRepository
 * ==============================================================================================
 * 
 * TARGETED @EntityGraph USAGE (Selective Eager Fetching):
 * - Applied ONLY where N+1 query multiplication actually occurs: batch queue
 * listings
 * where each ticket row references a different customer or agent.
 * - Omitted from single-entity lookups (findById) and specific filter methods
 * to avoid
 * unnecessary SQL LEFT JOIN overhead when relations are not needed.
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Selective join for customer tickets: fetches assignedAgent to avoid N queries
    // for agents
    @EntityGraph(attributePaths = { "assignedAgent" })
    List<Ticket> findByCustomerId(Long customerId);

    @EntityGraph(attributePaths = { "assignedAgent" })
    Page<Ticket> findByCustomerId(Long customerId, Pageable pageable);

    // Selective join for agent queue: fetches customer to avoid N queries for
    // customers
    @EntityGraph(attributePaths = { "customer" })
    List<Ticket> findByAssignedAgentId(Long agentId);

    @EntityGraph(attributePaths = { "customer" })
    Page<Ticket> findByAssignedAgentId(Long agentId, Pageable pageable);

    /**
     * WORKLOAD QUERY: Count active tickets (OPEN or IN_PROGRESS) assigned to a given agent.
     * Used by WorkloadBalancedRoutingStrategy to balance ticket assignments across the support team.
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedAgent.id = :agentId AND t.status IN ('OPEN', 'IN_PROGRESS')")
    long countActiveTicketsByAgentId(@Param("agentId") Long agentId);

    List<Ticket> findByStatus(TicketStatus status);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    List<Ticket> findByPriority(TicketPriority priority);

    List<Ticket> findByStatusAndPriority(TicketStatus status, TicketPriority priority);

    // Full relation join: Agent/admin queue where both customer and agent vary
    // across rows
    @EntityGraph(attributePaths = { "customer", "assignedAgent" })
    @Override
    List<Ticket> findAll();

    @EntityGraph(attributePaths = { "customer", "assignedAgent" })
    @Override
    Page<Ticket> findAll(Pageable pageable);

    @Query("SELECT t FROM Ticket t")
    List<Ticket> findAllIncludingDeleted();

    @Query("SELECT t FROM Ticket t WHERE t.customer.id = :customerId")
    List<Ticket> findByCustomerIdIncludingDeleted(@Param("customerId") Long customerId);

    /**
     * Paginated SLA breach detection query for background escalation scheduler.
     * Selectively fetches customer and assignedAgent to avoid N+1 queries during notification dispatch.
     */
    @EntityGraph(attributePaths = { "customer", "assignedAgent" })
    Page<Ticket> findByStatusInAndSlaDueAtBeforeAndSlaBreachedFalse(
            java.util.Collection<TicketStatus> statuses,
            java.time.LocalDateTime now,
            Pageable pageable);

    /**
     * Active tickets that have breached SLA (for queue filtering and dashboard metrics).
     */
    @EntityGraph(attributePaths = { "customer", "assignedAgent" })
    Page<Ticket> findByStatusInAndSlaBreachedTrue(
            java.util.Collection<TicketStatus> statuses,
            Pageable pageable);

    /**
     * Active tickets approaching SLA breach within the warning threshold window.
     */
    @EntityGraph(attributePaths = { "customer", "assignedAgent" })
    Page<Ticket> findByStatusInAndSlaBreachedFalseAndSlaDueAtBetween(
            java.util.Collection<TicketStatus> statuses,
            java.time.LocalDateTime start,
            java.time.LocalDateTime end,
            Pageable pageable);

    /**
     * SLA Metric Counts
     */
    long countByStatusInAndSlaBreachedTrue(java.util.Collection<TicketStatus> statuses);

    long countByStatusInAndSlaBreachedFalseAndSlaDueAtBetween(
            java.util.Collection<TicketStatus> statuses,
            java.time.LocalDateTime start,
            java.time.LocalDateTime end);

    long countByStatusIn(java.util.Collection<TicketStatus> statuses);

    long countByStatus(TicketStatus status);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = 'RESOLVED' AND t.slaBreached = false")
    long countResolvedWithinSla();

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = 'RESOLVED'")
    long countTotalResolved();
}