package com.support.observer.listener;

import com.support.config.AsyncConfig;
import com.support.observer.event.TicketAssignedEvent;
import com.support.observer.event.TicketMessageAddedEvent;
import com.support.observer.event.TicketStatusChangedEvent;
import com.support.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * ==============================================================================================
 * CONCRETE OBSERVER: EmailNotificationObserver (Asynchronous SMTP Notification Listener)
 * ==============================================================================================
 *
 * 1. HOW THIS OBSERVER WORKS:
 * ----------------------------------------------------------------------------------------------
 * This class acts as a concrete OBSERVER in the GoF Observer Pattern.
 * It subscribes to domain events broadcast by `TicketEventPublisher`.
 *
 * When an event occurs:
 * 1. Spring's event router intercepts the published event and calls the corresponding `@EventListener` method.
 * 2. `@Async(AsyncConfig.AUDIT_EXECUTOR)` ensures this execution occurs on a dedicated background thread.
 * 3. The method formats and dispatches the email via `EmailService`.
 *
 * 2. FAULT TOLERANCE & NON-BLOCKING ARCHITECTURE:
 * ----------------------------------------------------------------------------------------------
 * - The database transaction in `TicketService` commits immediately and returns to the client.
 * - If the external SMTP mail server experiences high latency or an outage, only this async observer
 *   logs a warning — the customer's HTTP request is NEVER blocked or failed.
 */
@Slf4j
@Component
public class EmailNotificationObserver {

    private final EmailService emailService;

    @Autowired
    public EmailNotificationObserver(EmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * Observer handler for ticket assignment events.
     * Notifies the assigned support agent via email.
     */
    @Async(AsyncConfig.AUDIT_EXECUTOR)
    @EventListener
    public void onTicketAssigned(TicketAssignedEvent event) {
        log.info("EmailObserver: Received TicketAssignedEvent for ticket id={}", event.getTicketId());
        try {
            if (event.getAgent() != null && event.getAgent().getEmail() != null) {
                emailService.sendTicketAssignedEmail(
                        event.getAgent().getEmail(),
                        event.getTicketId(),
                        event.getTicketTitle(),
                        event.getAgent().getUsername()
                );
            }
        } catch (Exception ex) {
            log.error("EmailObserver: Failed to send assignment email for ticket id={}: {}",
                    event.getTicketId(), ex.getMessage());
        }
    }

    /**
     * Observer handler for ticket status change events.
     * Notifies the customer and assigned agent of the lifecycle update.
     */
    @Async(AsyncConfig.AUDIT_EXECUTOR)
    @EventListener
    public void onTicketStatusChanged(TicketStatusChangedEvent event) {
        log.info("EmailObserver: Received TicketStatusChangedEvent for ticket id={}", event.getTicketId());
        try {
            // 1. Notify the customer
            if (event.getCustomer() != null && event.getCustomer().getEmail() != null) {
                emailService.sendTicketStatusChangedEmail(
                        event.getCustomer().getEmail(),
                        event.getTicketId(),
                        event.getTicketTitle(),
                        event.getOldStatus(),
                        event.getNewStatus()
                );
            }

            // 2. Notify assigned agent (if the change was not made by the agent themselves)
            Long changedByUserId = event.getChangedBy() != null ? event.getChangedBy().getId() : null;
            if (event.getAssignedAgent() != null &&
                    !event.getAssignedAgent().getId().equals(changedByUserId) &&
                    event.getAssignedAgent().getEmail() != null) {
                emailService.sendTicketStatusChangedEmail(
                        event.getAssignedAgent().getEmail(),
                        event.getTicketId(),
                        event.getTicketTitle(),
                        event.getOldStatus(),
                        event.getNewStatus()
                );
            }
        } catch (Exception ex) {
            log.error("EmailObserver: Failed to send status change email for ticket id={}: {}",
                    event.getTicketId(), ex.getMessage());
        }
    }

    /**
     * Observer handler for conversation reply events.
     * Notifies the counterpart recipient of the incoming message.
     */
    @Async(AsyncConfig.AUDIT_EXECUTOR)
    @EventListener
    public void onTicketMessageAdded(TicketMessageAddedEvent event) {
        log.info("EmailObserver: Received TicketMessageAddedEvent for ticket id={}", event.getTicketId());
        try {
            if (event.getRecipient() != null && event.getRecipient().getEmail() != null) {
                emailService.sendTicketReplyEmail(
                        event.getRecipient().getEmail(),
                        event.getTicketId(),
                        event.getTicketTitle(),
                        event.getSender().getUsername(),
                        event.getMessage().getContent()
                );
            }
        } catch (Exception ex) {
            log.error("EmailObserver: Failed to send reply notification email for ticket id={}: {}",
                    event.getTicketId(), ex.getMessage());
        }
    }
}
