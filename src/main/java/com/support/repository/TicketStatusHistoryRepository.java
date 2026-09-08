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
 * TARGETED @EntityGraph USAGE:
 * - Applied only to `findByTicketIdOrderByChangedAtAsc` to avoid N queries for changedBy user
 *   when rendering multi-entry audit timelines.
 */
@Repository
public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistory, Long> {

    @EntityGraph(attributePaths = {"changedBy"})
    List<TicketStatusHistory> findByTicketIdOrderByChangedAtAsc(Long ticketId);

    List<TicketStatusHistory> findByNewStatus(TicketStatus newStatus);

    TicketStatusHistory findTop1ByTicketIdOrderByChangedAtDesc(Long ticketId);
}
