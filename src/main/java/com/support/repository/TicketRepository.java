package com.support.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.support.entity.Ticket;
import com.support.entity.TicketPriority;
import com.support.entity.TicketStatus;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByCustomerId(Long customerId);

    List<Ticket> findByAssignedAgentId(Long agentId);

    List<Ticket> findByStatus(TicketStatus status);

    List<Ticket> findByPriority(TicketPriority priority);

    List<Ticket> findByStatusAndPriority(TicketStatus status, TicketPriority priority);
}