package com.support.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.support.entity.TicketStatusHistory;
import com.support.entity.TicketStatus;

@Repository
public interface TicketStatusHistoryRepository extends JpaRepository<TicketStatusHistory, Long> {

    List<TicketStatusHistory> findByTicketIdOrderByChangedAtAsc(Long ticketId);

    List<TicketStatusHistory> findByNewStatus(TicketStatus newStatus);

    TicketStatusHistory findTop1ByTicketIdOrderByChangedAtDesc(Long ticketId);
}
