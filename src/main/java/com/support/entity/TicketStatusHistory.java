package com.support.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * ==============================================================================================
 * ENTITY: TicketStatusHistory (State Transition Audit Log)
 * ==============================================================================================
 * 
 * WHY THIS DATABASE DESIGN:
 * - `idx_tsh_ticket_id`: Fast retrieval of complete status audit timeline for any specific ticket.
 */
@Entity
@Table(name = "ticket_status_history", indexes = {
    @Index(name = "idx_tsh_ticket_id", columnList = "ticket_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status")
    private TicketStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private TicketStatus newStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_id", nullable = false)
    private User changedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        this.changedAt = LocalDateTime.now();
    }
}
