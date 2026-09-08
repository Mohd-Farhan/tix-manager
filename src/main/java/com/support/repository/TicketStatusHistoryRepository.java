package com.support.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.support.entity.TicketStatusHistory;
import com.support.entity.TicketStatus;

/**
 * ==============================================================================================
 * REPOSITORY: TicketStatusHistoryRepository
 * ==============================================================================================
 * 
 * WHY QUERY OPTIMIZATION & @EntityGraph ARE USED:
 * - `@EntityGraph(attributePaths = {"changedBy"})` loads the actor who triggered the transition
 *   eagerly in a single JOIN.
 */
@Repository
public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistory, Long> {

    @EntityGraph(attributePaths = {"changedBy"})
    List<TicketStatusHistory> findByTicketIdOrderByChangedAtAsc(Long ticketId);

    List<TicketStatusHistory> findByNewStatus(TicketStatus newStatus);

    @EntityGraph(attributePaths = {"changedBy"})
    TicketStatusHistory findTop1ByTicketIdOrderByChangedAtDesc(Long ticketId);
}
