package com.support.observer.event;

import com.support.entity.TicketStatus;
import com.support.entity.User;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * ==============================================================================================
 * DOMAIN EVENT: TicketStatusChangedEvent
 * ==============================================================================================
 *
 * Broadcast when a ticket transitions across lifecycle states (e.g., OPEN -> IN_PROGRESS -> RESOLVED).
 * Observers listen to this event to notify the customer and assigned agent of the progress.
 */
@Getter
@ToString
public class TicketStatusChangedEvent implements TicketEvent {

    private final Long ticketId;
    private final String ticketTitle;
    private final TicketStatus oldStatus;
    private final TicketStatus newStatus;
    private final User customer;
    private final User assignedAgent;
    private final User changedBy;
    private final LocalDateTime timestamp;

    public TicketStatusChangedEvent(Long ticketId, String ticketTitle, TicketStatus oldStatus,
                                     TicketStatus newStatus, User customer, User assignedAgent, User changedBy) {
        this.ticketId = ticketId;
        this.ticketTitle = ticketTitle;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.customer = customer;
        this.assignedAgent = assignedAgent;
        this.changedBy = changedBy;
        this.timestamp = LocalDateTime.now();
    }
}
