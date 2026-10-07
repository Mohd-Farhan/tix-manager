package com.support.observer.listener;

import com.support.config.AsyncConfig;
import com.support.entity.Notification;
import com.support.entity.NotificationType;
import com.support.observer.event.TicketAssignedEvent;
import com.support.observer.event.TicketMessageAddedEvent;
import com.support.observer.event.TicketStatusChangedEvent;
import com.support.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * ==============================================================================================
 * CONCRETE OBSERVER: InAppNotificationObserver (Persistent In-App Alerts Listener)
 * ==============================================================================================
 *
 * 1. HOW THIS OBSERVER WORKS:
 * ----------------------------------------------------------------------------------------------
 * This class acts as a SECOND independent OBSERVER in the GoF Observer Pattern.
 * Notice that it listens to the exact same domain events (`TicketAssignedEvent`, etc.) as
 * `EmailNotificationObserver`, but performs a completely different responsibility:
 * -> It persists in-app notification rows into the database!
 *
 * 2. DEMONSTRATION OF OPEN-CLOSED PRINCIPLE (OCP):
 * ----------------------------------------------------------------------------------------------
 * Neither `TicketService` nor `EmailNotificationObserver` knows this class exists.
 * Adding in-app alerts required ZERO changes to the email sending logic or ticket operations!
 */
@Slf4j
@Component
public class InAppNotificationObserver {

    private final NotificationRepository notificationRepository;

    @Autowired
    public InAppNotificationObserver(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    /**
     * Observer handler for ticket assignment.
     * Records an in-app alert in the assigned agent's notification feed.
     */
    @Async(AsyncConfig.AUDIT_EXECUTOR)
    @EventListener
    public void onTicketAssigned(TicketAssignedEvent event) {
        log.info("InAppObserver: Creating in-app notification for agent id={} on ticket id={}",
                event.getAgent().getId(), event.getTicketId());
        try {
            if (event.getAgent() != null) {
                Notification notification = Notification.builder()
                        .recipient(event.getAgent())
                        .title(String.format("Ticket Assigned: #%d", event.getTicketId()))
                        .message(String.format("You were assigned to ticket '%s' by %s.",
                                event.getTicketTitle(), event.getAssignedBy()))
                        .type(NotificationType.TICKET_ASSIGNED)
                        .ticketId(event.getTicketId())
                        .read(false)
                        .build();

                notificationRepository.save(notification);
            }
        } catch (Exception ex) {
            log.error("InAppObserver: Failed to save assignment notification: {}", ex.getMessage());
        }
    }

    /**
     * Observer handler for ticket status changes.
     * Records in-app alerts for both the customer and the assigned agent.
     */
    @Async(AsyncConfig.AUDIT_EXECUTOR)
    @EventListener
    public void onTicketStatusChanged(TicketStatusChangedEvent event) {
        log.info("InAppObserver: Creating in-app notifications for status change on ticket id={}", event.getTicketId());
        try {
            // 1. Notify Customer
            if (event.getCustomer() != null) {
                Notification customerAlert = Notification.builder()
                        .recipient(event.getCustomer())
                        .title(String.format("Ticket #%d Status: %s", event.getTicketId(), event.getNewStatus()))
                        .message(String.format("Your ticket '%s' status was updated from %s to %s.",
                                event.getTicketTitle(), event.getOldStatus(), event.getNewStatus()))
                        .type(NotificationType.STATUS_CHANGED)
                        .ticketId(event.getTicketId())
                        .read(false)
                        .build();
                notificationRepository.save(customerAlert);
            }

            // 2. Notify Assigned Agent (if someone else changed it)
            Long changedByUserId = event.getChangedBy() != null ? event.getChangedBy().getId() : null;
            if (event.getAssignedAgent() != null && !event.getAssignedAgent().getId().equals(changedByUserId)) {
                Notification agentAlert = Notification.builder()
                        .recipient(event.getAssignedAgent())
                        .title(String.format("Ticket #%d Status: %s", event.getTicketId(), event.getNewStatus()))
                        .message(String.format("Assigned ticket '%s' status updated to %s by %s.",
                                event.getTicketTitle(), event.getNewStatus(),
                                event.getChangedBy() != null ? event.getChangedBy().getUsername() : "System"))
                        .type(NotificationType.STATUS_CHANGED)
                        .ticketId(event.getTicketId())
                        .read(false)
                        .build();
                notificationRepository.save(agentAlert);
            }
        } catch (Exception ex) {
            log.error("InAppObserver: Failed to save status change notification: {}", ex.getMessage());
        }
    }

    /**
     * Observer handler for new replies.
     * Records an in-app alert for the recipient.
     */
    @Async(AsyncConfig.AUDIT_EXECUTOR)
    @EventListener
    public void onTicketMessageAdded(TicketMessageAddedEvent event) {
        log.info("InAppObserver: Creating in-app notification for message reply on ticket id={}", event.getTicketId());
        try {
            if (event.getRecipient() != null) {
                String snippet = event.getMessage() != null && event.getMessage().getContent() != null
                        ? truncate(event.getMessage().getContent(), 100)
                        : "New message posted";

                Notification replyAlert = Notification.builder()
                        .recipient(event.getRecipient())
                        .title(String.format("New Reply on Ticket #%d", event.getTicketId()))
                        .message(String.format("%s: %s", event.getSender().getUsername(), snippet))
                        .type(NotificationType.NEW_MESSAGE)
                        .ticketId(event.getTicketId())
                        .read(false)
                        .build();

                notificationRepository.save(replyAlert);
            }
        } catch (Exception ex) {
            log.error("InAppObserver: Failed to save message notification: {}", ex.getMessage());
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
