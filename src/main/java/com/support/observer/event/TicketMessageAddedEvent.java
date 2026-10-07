package com.support.observer.event;

import com.support.entity.Message;
import com.support.entity.User;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * ==============================================================================================
 * DOMAIN EVENT: TicketMessageAddedEvent
 * ==============================================================================================
 *
 * Broadcast when a new message is posted to a ticket thread by a customer or an agent.
 * Observers listen to this event to notify the opposite party (customer <-> agent).
 */
@Getter
@ToString
public class TicketMessageAddedEvent implements TicketEvent {

    private final Long ticketId;
    private final String ticketTitle;
    private final Message message;
    private final User sender;
    private final User recipient;
    private final LocalDateTime timestamp;

    public TicketMessageAddedEvent(Long ticketId, String ticketTitle, Message message, User sender, User recipient) {
        this.ticketId = ticketId;
        this.ticketTitle = ticketTitle;
        this.message = message;
        this.sender = sender;
        this.recipient = recipient;
        this.timestamp = LocalDateTime.now();
    }
}
