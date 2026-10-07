package com.support.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * ==============================================================================================
 * ENTITY: Notification (In-App Alert Persistence)
 * ==============================================================================================
 *
 * DESIGN PATTERN CONTEXT (Observer Pattern):
 * When domain events occur (e.g. TicketAssignedEvent, TicketStatusChangedEvent),
 * the InAppNotificationObserver creates and persists rows in this table.
 *
 * Users can view their unread notification badge, list alerts, and mark them as read in the UI.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_recipient", columnList = "recipient_id"),
        @Index(name = "idx_notification_read", columnList = "recipient_id, is_read"),
        @Index(name = "idx_notification_created_at", columnList = "created_at DESC")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(name = "ticket_id")
    private Long ticketId;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;
}
