package com.support.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * ==============================================================================================
 * ENTITY: AuditLog (Unified Domain Entity Audit Trail)
 * ==============================================================================================
 * 
 * ARCHITECTURAL DECISION — WHY A UNIFIED AUDIT TABLE INSTEAD OF HIBERNATE ENVERS:
 * 
 * 1. Schema Overhead & Table Proliferation:
 *    Hibernate Envers creates shadow tables for every single audited entity (e.g. `users_aud`,
 *    `tickets_aud`, `messages_aud`, `revinfo`). As entity counts grow, schema maintenance doubles.
 * 
 * 2. Cross-Entity Unified Dashboard Queries:
 *    Querying an activity stream across multiple entities (e.g. Admin timeline: "Who did what recently?")
 *    requires expensive, complex multi-table UNION queries across N Envers tables. A single unified
 *    `audit_logs` table allows simple, high-performance indexed queries (`ORDER BY created_at DESC`).
 * 
 * 3. Distributed Tracing & Correlation ID:
 *    Envers relies on its internal RevInfo listener. Injecting custom distributed trace IDs
 *    (X-Correlation-ID / SLF4J MDC) requires non-trivial custom listeners, whereas this unified
 *    table directly stores `traceId` linking DB changes directly to Logback rotated files.
 * 
 * 4. Human-Readable Business Semantics vs Raw Column Diffs:
 *    Envers records low-level raw database column snapshots on every flush. This table records
 *    high-level business actions (e.g. 'ASSIGN_AGENT', 'STATUS_TRANSITION', 'BULK_UPLOAD') with
 *    contextual human-readable descriptions.
 */
@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_entity", columnList = "entity_name, entity_id"),
    @Index(name = "idx_audit_created_at", columnList = "created_at DESC")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_name", nullable = false, length = 50)
    private String entityName; // 'USER', 'TICKET', 'MESSAGE'

    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "action", nullable = false, length = 50)
    private String action; // 'CREATE', 'UPDATE', 'DELETE', 'STATUS_CHANGE', 'ASSIGN'

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
