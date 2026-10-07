package com.support.observer.event;

import com.support.entity.User;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * ==============================================================================================
 * DOMAIN EVENT: TicketAssignedEvent
 * ==============================================================================================
 *
 * Broadcast when a support ticket is assigned or reassigned to a support agent.
 * Observers listen to this event to send notification emails and create in-app alerts.
 */
@Getter
@ToString
public class TicketAssignedEvent implements TicketEvent {

    private final Long ticketId;
    private final String ticketTitle;
    private final User agent;
    private final String assignedBy;
    private final LocalDateTime timestamp;

    public TicketAssignedEvent(Long ticketId, String ticketTitle, User agent, String assignedBy) {
        this.ticketId = ticketId;
        this.ticketTitle = ticketTitle;
        this.agent = agent;
        this.assignedBy = assignedBy;
        this.timestamp = LocalDateTime.now();
    }
}
