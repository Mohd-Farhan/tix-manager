package com.support.observer.event;

import java.time.LocalDateTime;

/**
 * ==============================================================================================
 * GOF BEHAVIORAL PATTERN: OBSERVER PATTERN (Domain Event Contract)
 * ==============================================================================================
 *
 * 1. WHAT IS THE OBSERVER PATTERN?
 * ----------------------------------------------------------------------------------------------
 * The Observer Pattern (Gang of Four) is a behavioral design pattern where an object (the
 * SUBJECT or PUBLISHER) maintains a list of dependents (the OBSERVERS or SUBSCRIBERS) and notifies
 * them automatically of any state changes, typically by broadcasting an event.
 *
 * In classic enterprise Java / Spring Boot:
 * - SUBJECT / PUBLISHER: `com.support.observer.publisher.TicketEventPublisher`
 *   (Uses Spring's `ApplicationEventPublisher` to broadcast domain events).
 * - EVENT OBJECT: The payload describing what happened (`TicketEvent` implementations).
 * - OBSERVERS / LISTENERS: Classes that subscribe to events:
 *   * `EmailNotificationObserver`: Sends asynchronous SMTP emails via `EmailService`.
 *   * `InAppNotificationObserver`: Persists in-app notifications into the database.
 *   * (Future) `SlackNotificationObserver`, `SmsNotificationObserver`, etc.
 *
 * 2. WHY USE THE OBSERVER PATTERN INSTEAD OF DIRECT SERVICE CALLS?
 * ----------------------------------------------------------------------------------------------
 * Without the Observer Pattern:
 *
 *   public TicketResponse updateTicketStatus(...) {
 *       // update database
 *       emailService.sendTicketStatusChangedEmail(...);
 *       inAppNotificationService.createAlert(...);
 *       slackWebhookService.sendAlert(...);
 *       auditLogService.recordEvent(...);
 *       metricsService.incrementCounter(...);
 *   }
 *
 * Problems with direct coupling:
 *  a. Violates Single Responsibility Principle (SRP):
 *     `TicketService` becomes a dumping ground for all notification channels, email formatting,
 *     and webhook mechanics instead of focusing solely on ticket core logic.
 *  b. Violates Open-Closed Principle (OCP):
 *     Adding a new notification channel (e.g. mobile push or webhooks) requires modifying and
 *     re-testing `TicketService`.
 *  c. Performance & Fault Isolation:
 *     If an external mail server or webhook is slow or times out, the main HTTP transaction is
 *     blocked or fails. With asynchronous observers (`@Async`), notifications run in the background
 *     on a worker thread pool, keeping client response times sub-millisecond and isolating failures.
 * ==============================================================================================
 */
public interface TicketEvent {

    /**
     * Unique identifier of the ticket associated with this event.
     */
    Long getTicketId();

    /**
     * Title of the ticket associated with this event.
     */
    String getTicketTitle();

    /**
     * Timestamp when the domain event occurred.
     */
    LocalDateTime getTimestamp();
}
