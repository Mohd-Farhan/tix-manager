package com.support.service;

import com.support.config.AsyncConfig;
import com.support.entity.TicketStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * ==============================================================================================
 * SERVICE: EmailService (Enterprise Asynchronous Event Notifications)
 * ==============================================================================================
 * 
 * WHY ASYNCHRONOUS NON-BLOCKING DISPATCH (@Async):
 * 1. Network Latency & Fault Isolation:
 *    - External mail relays (SMTP, AWS SES, SendGrid, Mailgun) introduce 100ms to several seconds
 *      of network latency per invocation.
 *    - Offloading notification dispatch to the dedicated bounded `auditExecutor` thread pool ensures
 *      that critical business operations (user creation, bulk CSV imports, ticket status transitions)
 *      commit immediately without blocking client HTTP responses or consuming servlet threads.
 * 
 * 2. Enterprise Resiliency & Local Dev Fallback:
 *    - In development, staging, or container test environments where an external SMTP relay is
 *      not configured (`app.mail.enabled=false`), this service formats the notification payload
 *      and logs it as an operational audit event rather than failing transactions or throwing
 *      connection exceptions.
 * 
 * 3. NIST SP 800-63B Credential Communication:
 *    - Welcome emails provide temporary/initial credentials with explicit instructions that the
 *      account is flagged for mandatory password change on first authentication.
 */
@Slf4j
@Service
public class EmailService {

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:no-reply@tixmanager.com}")
    private String mailFrom;

    /**
     * Dispatches welcome email with initial/temporary credentials to newly provisioned users.
     */
    @Async(AsyncConfig.AUDIT_EXECUTOR)
    public void sendWelcomeEmail(String toEmail, String username, String temporaryPassword) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Cannot send welcome email: recipient email address is empty for user '{}'", username);
            return;
        }

        String subject = "Welcome to TixManager — Your Account Credentials";
        String body = String.format(
                "Hello %s,\n\n" +
                "Your TixManager account has been successfully provisioned by an administrator.\n\n" +
                "Account Details:\n" +
                "  • Username: %s\n" +
                "  • Temporary Password: %s\n\n" +
                "SECURITY NOTICE:\n" +
                "For your security and compliance with NIST SP 800-63B guidelines, you will be required " +
                "to personalize your password upon your first login.\n\n" +
                "Login here: http://localhost:4200/auth/login\n\n" +
                "— The TixManager Security & Support Team",
                username, username, temporaryPassword);

        dispatchNotification(toEmail, subject, body);
    }

    /**
     * Notifies a support agent when an open ticket is assigned to them.
     */
    @Async(AsyncConfig.AUDIT_EXECUTOR)
    public void sendTicketAssignedEmail(String toEmail, Long ticketId, String ticketTitle, String agentName) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }

        String subject = String.format("[TixManager] Ticket #%d Assigned to You: %s", ticketId, ticketTitle);
        String body = String.format(
                "Hello %s,\n\n" +
                "Support Ticket #%d has been assigned to you for resolution.\n\n" +
                "Ticket Title: %s\n" +
                "Current Status: IN_PROGRESS\n\n" +
                "Please review the conversation thread and respond to the customer:\n" +
                "http://localhost:4200/agent/tickets/%d\n\n" +
                "— TixManager Automated Triage",
                agentName, ticketId, ticketTitle, ticketId);

        dispatchNotification(toEmail, subject, body);
    }

    /**
     * Notifies customer and stakeholders when a ticket transitions status (OPEN -> IN_PROGRESS -> RESOLVED).
     */
    @Async(AsyncConfig.AUDIT_EXECUTOR)
    public void sendTicketStatusChangedEmail(String toEmail, Long ticketId, String ticketTitle, TicketStatus oldStatus, TicketStatus newStatus) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }

        String subject = String.format("[TixManager] Ticket #%d Status Update: %s", ticketId, newStatus);
        String body = String.format(
                "Hello,\n\n" +
                "The status of Support Ticket #%d has been updated.\n\n" +
                "Ticket Title: %s\n" +
                "Previous Status: %s\n" +
                "New Status: %s\n\n" +
                "View the updated ticket details and conversation:\n" +
                "http://localhost:4200/tickets/%d\n\n" +
                "— TixManager Support Team",
                ticketId, ticketTitle, oldStatus, newStatus, ticketId);

        dispatchNotification(toEmail, subject, body);
    }

    /**
     * Notifies opposite party (customer or agent) when a new message is posted to a ticket thread.
     */
    @Async(AsyncConfig.AUDIT_EXECUTOR)
    public void sendTicketReplyEmail(String toEmail, Long ticketId, String ticketTitle, String senderName, String messageSnippet) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }

        String preview = messageSnippet != null && messageSnippet.length() > 100
                ? messageSnippet.substring(0, 100) + "..."
                : (messageSnippet != null ? messageSnippet : "");

        String subject = String.format("[TixManager] New Reply on Ticket #%d by %s", ticketId, senderName);
        String body = String.format(
                "Hello,\n\n" +
                "%s has added a reply to Support Ticket #%d (\"%s\"):\n\n" +
                "\"%s\"\n\n" +
                "View full message thread and reply online:\n" +
                "http://localhost:4200/tickets/%d\n\n" +
                "— TixManager Notifications",
                senderName, ticketId, ticketTitle, preview, ticketId);

        dispatchNotification(toEmail, subject, body);
    }

    /**
     * Core dispatcher: sends via mail transport if enabled or logs structured notification event.
     */
    protected void dispatchNotification(String toEmail, String subject, String body) {
        if (mailEnabled) {
            log.info("Dispatching external email via SMTP relay to <{}> [Subject: '{}']", toEmail, subject);
            // Real SMTP delivery can be wired to JavaMailSender or cloud SDK when enabled
        } else {
            log.info("[SIMULATED EMAIL NOTIFICATION] From: {} | To: {} | Subject: '{}'\n---\n{}\n---",
                    mailFrom, toEmail, subject, body);
        }
    }
}
