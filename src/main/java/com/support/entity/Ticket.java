package com.support.entity;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * ==============================================================================================
 * ENTITY: Ticket (Domain Model for Customer Support Tickets)
 * ==============================================================================================
 * 
 * WHY THIS DATABASE & CONCURRENCY DESIGN (Enterprise Standard):
 * 
 * 1. Optimistic Locking (@Version):
 * - Prevents "lost updates" and race conditions when multiple support agents or
 * customers
 * attempt to update/assign the same ticket simultaneously.
 * - Hibernate verifies the version column during UPDATE SQL. If another
 * transaction changed
 * the row first, an OptimisticLockException is thrown and caught by
 * GlobalExceptionHandler.
 * 
 * 2. Targeted Database Indexing:
 * - `idx_ticket_customer`: Accelerates customer portal lookups (WHERE
 * customer_id = ?).
 * - `idx_ticket_agent`: Accelerates agent queue lookups (WHERE agent_id = ?).
 * - `idx_ticket_status`: Accelerates active queue filtering (WHERE status = ?).
 * - Low-cardinality columns (deleted, priority) are deliberately not indexed to
 * avoid write overhead.
 * 
 * 3. Soft Deletion & Audit Trail:
 * - `@SQLRestriction("deleted = false")` guarantees transparent exclusion of
 * soft-deleted rows.
 */
@Entity
@Table(name = "tickets", indexes = {
        @Index(name = "idx_ticket_customer", columnList = "customer_id"),
        @Index(name = "idx_ticket_agent", columnList = "agent_id"),
        @Index(name = "idx_ticket_status", columnList = "status")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted = false")
public class Ticket extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Optimistic Locking Version Counter.
     * Automatically incremented by JPA on every UPDATE.
     */
    @Version
    private Long version;

    @Column(nullable = false)
    @NotBlank
    @Size(max = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketPriority priority = TicketPriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private User assignedAgent;

    @Column(nullable = false)
    private boolean deleted = false;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TicketStatusHistory> statusHistory = new ArrayList<>();
}