package com.support.repository;

import java.util.List;
import java.util.Optional;

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
 * WHY QUERY OPTIMIZATION & @EntityGraph ARE USED (N+1 Problem Prevention):
 * 1. Single-Query Fetching:
 *    - By default, lazy-loaded relations (`customer`, `assignedAgent`) cause Hibernate to execute
 *      1 query for tickets + N queries for each ticket's customer/agent (the "N+1 query anti-pattern").
 *    - `@EntityGraph(attributePaths = {"customer", "assignedAgent"})` forces Hibernate to generate
 *      a single SQL `LEFT OUTER JOIN`, reducing 50+ database roundtrips down to 1 roundtrip.
 * 2. Pagination & Sorting:
 *    - Supports `Pageable` parameters for scalable, memory-efficient queries on large datasets.
 */
@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    Optional<Ticket> findById(Long id);

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    List<Ticket> findByCustomerId(Long customerId);

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    Page<Ticket> findByCustomerId(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    List<Ticket> findByAssignedAgentId(Long agentId);

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    Page<Ticket> findByAssignedAgentId(Long agentId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    List<Ticket> findByStatus(TicketStatus status);

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    List<Ticket> findByPriority(TicketPriority priority);

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    List<Ticket> findByStatusAndPriority(TicketStatus status, TicketPriority priority);

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    @Override
    List<Ticket> findAll();

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    @Override
    Page<Ticket> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    @Query("SELECT t FROM Ticket t")
    List<Ticket> findAllIncludingDeleted();

    @EntityGraph(attributePaths = {"customer", "assignedAgent"})
    @Query("SELECT t FROM Ticket t WHERE t.customer.id = :customerId")
    List<Ticket> findByCustomerIdIncludingDeleted(@Param("customerId") Long customerId);
}