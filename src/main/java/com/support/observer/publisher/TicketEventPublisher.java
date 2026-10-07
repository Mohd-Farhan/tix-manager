package com.support.observer.publisher;

import com.support.entity.Message;
import com.support.entity.Ticket;
import com.support.entity.TicketStatus;
import com.support.entity.User;
import com.support.observer.event.TicketAssignedEvent;
import com.support.observer.event.TicketMessageAddedEvent;
import com.support.observer.event.TicketStatusChangedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * ==============================================================================================
 * GOF BEHAVIORAL PATTERN: OBSERVER PATTERN (Subject / Event Publisher)
 * ==============================================================================================
 *
 * 1. WHAT ROLE DOES THIS CLASS PLAY?
 * ----------------------------------------------------------------------------------------------
 * In the GoF Observer Pattern, the SUBJECT is the entity that holds state and notifies observers
 * when changes occur.
 *
 * Here, `TicketEventPublisher`:
 * - Acts as the centralized Subject / Event Bus interface.
 * - Wraps Spring's `ApplicationEventPublisher`.
 * - Translates low-level business actions into rich, strongly-typed domain events.
 * - Completely decouples `TicketService` from knowing who is listening or how notifications
 *   are delivered (Email, Database In-App alert, SMS, Webhook, etc.).
 *
 * 2. SPRING APPLICATION EVENT PUBLISHER MECHANICS:
 * ----------------------------------------------------------------------------------------------
 * Calling `eventPublisher.publishEvent(...)` locates all `@EventListener` beans subscribed to
 * that event class. Spring dispatches the event to each observer. Because observers are marked
 * `@Async`, they execute on the dedicated background thread pool (`auditExecutor`) without
 * delaying the client HTTP response.
 */
@Slf4j
@Component
public class TicketEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public TicketEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Broadcasts that a ticket has been assigned to an agent.
     */
    public void publishTicketAssigned(Ticket ticket, User agent, String assignedBy) {
        TicketAssignedEvent event = new TicketAssignedEvent(
                ticket.getId(),
                ticket.getTitle(),
                agent,
                assignedBy
        );
        log.info("Observer Subject: Publishing TicketAssignedEvent for ticket id={}", ticket.getId());
        eventPublisher.publishEvent(event);
    }

    /**
     * Broadcasts that a ticket's lifecycle status has transitioned.
     */
    public void publishTicketStatusChanged(Ticket ticket, TicketStatus oldStatus, TicketStatus newStatus, User changedBy) {
        TicketStatusChangedEvent event = new TicketStatusChangedEvent(
                ticket.getId(),
                ticket.getTitle(),
                oldStatus,
                newStatus,
                ticket.getCustomer(),
                ticket.getAssignedAgent(),
                changedBy
        );
        log.info("Observer Subject: Publishing TicketStatusChangedEvent for ticket id={} ({} -> {})",
                ticket.getId(), oldStatus, newStatus);
        eventPublisher.publishEvent(event);
    }

    /**
     * Broadcasts that a new message has been added to a ticket thread.
     */
    public void publishTicketMessageAdded(Ticket ticket, Message message, User sender, User recipient) {
        TicketMessageAddedEvent event = new TicketMessageAddedEvent(
                ticket.getId(),
                ticket.getTitle(),
                message,
                sender,
                recipient
        );
        log.info("Observer Subject: Publishing TicketMessageAddedEvent for ticket id={} from sender='{}'",
                ticket.getId(), sender.getUsername());
        eventPublisher.publishEvent(event);
    }
}
