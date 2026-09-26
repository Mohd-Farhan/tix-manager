package com.support.service;

import com.support.config.AsyncConfig;
import com.support.entity.TicketStatus;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.charset.StandardCharsets;

/**
 * ==============================================================================================
 * SERVICE: EmailService (Enterprise Asynchronous Event Notifications & SMTP Relay)
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
 * 3. MIME Multipart & HTML Email Rendering:
 *    - Uses Thymeleaf `TemplateEngine` to generate responsive HTML emails with Azure Professional styling.
 *    - Sends both HTML and fallback plain text via `MimeMessageHelper.setText(plainText, htmlText)`.
 * 
 * 4. NIST SP 800-63B Credential Communication:
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

    @Value("${app.mail.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired(required = false)
    private TemplateEngine templateEngine;

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
        String loginUrl = frontendBaseUrl + "/auth/login";

        String textBody = String.format(
                "Hello %s,\n\n" +
                "Your TixManager account has been successfully provisioned by an administrator.\n\n" +
                "Account Details:\n" +
                "  • Username: %s\n" +
                "  • Temporary Password: %s\n\n" +
                "SECURITY NOTICE:\n" +
                "For your security and compliance with NIST SP 800-63B guidelines, you will be required " +
                "to personalize your password upon your first login.\n\n" +
                "Login here: %s\n\n" +
                "— The TixManager Security & Support Team",
                username, username, temporaryPassword, loginUrl);

        String htmlBody = null;
        if (templateEngine != null) {
            try {
                Context context = new Context();
                context.setVariable("username", username);
                context.setVariable("temporaryPassword", temporaryPassword);
                context.setVariable("loginUrl", loginUrl);
                htmlBody = templateEngine.process("email/welcome-email", context);
            } catch (Exception e) {
                log.warn("Failed to render welcome-email HTML template for '{}', falling back to plain text: {}", username, e.getMessage());
            }
        }

        dispatchNotification(toEmail, subject, textBody, htmlBody);
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
        String ticketUrl = String.format("%s/agent/tickets/%d", frontendBaseUrl, ticketId);

        String textBody = String.format(
                "Hello %s,\n\n" +
                "Support Ticket #%d has been assigned to you for resolution.\n\n" +
                "Ticket Title: %s\n" +
                "Current Status: IN_PROGRESS\n\n" +
                "Please review the conversation thread and respond to the customer:\n" +
                "%s\n\n" +
                "— TixManager Automated Triage",
                agentName, ticketId, ticketTitle, ticketUrl);

        String htmlBody = null;
        if (templateEngine != null) {
            try {
                Context context = new Context();
                context.setVariable("agentName", agentName);
                context.setVariable("ticketId", ticketId);
                context.setVariable("ticketTitle", ticketTitle);
                context.setVariable("ticketUrl", ticketUrl);
                htmlBody = templateEngine.process("email/ticket-assigned-email", context);
            } catch (Exception e) {
                log.warn("Failed to render ticket-assigned-email HTML template for ticket #{}: {}", ticketId, e.getMessage());
            }
        }

        dispatchNotification(toEmail, subject, textBody, htmlBody);
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
        String ticketUrl = String.format("%s/tickets/%d", frontendBaseUrl, ticketId);

        String textBody = String.format(
                "Hello,\n\n" +
                "The status of Support Ticket #%d has been updated.\n\n" +
                "Ticket Title: %s\n" +
                "Previous Status: %s\n" +
                "New Status: %s\n\n" +
                "View the updated ticket details and conversation:\n" +
                "%s\n\n" +
                "— TixManager Support Team",
                ticketId, ticketTitle, oldStatus, newStatus, ticketUrl);

        String htmlBody = null;
        if (templateEngine != null) {
            try {
                Context context = new Context();
                context.setVariable("ticketId", ticketId);
                context.setVariable("ticketTitle", ticketTitle);
                context.setVariable("oldStatus", oldStatus != null ? oldStatus.name() : "");
                context.setVariable("newStatus", newStatus != null ? newStatus.name() : "");
                context.setVariable("ticketUrl", ticketUrl);
                htmlBody = templateEngine.process("email/ticket-status-changed-email", context);
            } catch (Exception e) {
                log.warn("Failed to render ticket-status-changed-email HTML template for ticket #{}: {}", ticketId, e.getMessage());
            }
        }

        dispatchNotification(toEmail, subject, textBody, htmlBody);
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
        String ticketUrl = String.format("%s/tickets/%d", frontendBaseUrl, ticketId);

        String textBody = String.format(
                "Hello,\n\n" +
                "%s has added a reply to Support Ticket #%d (\"%s\"):\n\n" +
                "\"%s\"\n\n" +
                "View full message thread and reply online:\n" +
                "%s\n\n" +
                "— TixManager Notifications",
                senderName, ticketId, ticketTitle, preview, ticketUrl);

        String htmlBody = null;
        if (templateEngine != null) {
            try {
                Context context = new Context();
                context.setVariable("ticketId", ticketId);
                context.setVariable("ticketTitle", ticketTitle);
                context.setVariable("senderName", senderName);
                context.setVariable("messageSnippet", preview);
                context.setVariable("ticketUrl", ticketUrl);
                htmlBody = templateEngine.process("email/ticket-reply-email", context);
            } catch (Exception e) {
                log.warn("Failed to render ticket-reply-email HTML template for ticket #{}: {}", ticketId, e.getMessage());
            }
        }

        dispatchNotification(toEmail, subject, textBody, htmlBody);
    }

    /**
     * Overload for plain text backwards compatibility.
     */
    protected void dispatchNotification(String toEmail, String subject, String body) {
        dispatchNotification(toEmail, subject, body, null);
    }

    /**
     * Notifies assigned agent or administrators when an active ticket breaches its resolution SLA deadline.
     */
    @Async(AsyncConfig.AUDIT_EXECUTOR)
    public void sendSlaBreachEmail(String toEmail, Long ticketId, String ticketTitle, String priority, String dueAtFormatted, boolean escalated) {
        if (toEmail == null || toEmail.isBlank()) {
            return;
        }

        String subject = String.format("[SLA BREACH ALERT] Ticket #%d has breached resolution deadline", ticketId);
        String ticketUrl = String.format("%s/tickets/%d", frontendBaseUrl, ticketId);

        String textBody = String.format(
                "URGENT SLA BREACH NOTICE\n\n" +
                "Support Ticket #%d has exceeded its Service Level Agreement resolution deadline.\n\n" +
                "Ticket Title: %s\n" +
                "Current Priority: %s\n" +
                "SLA Target Due: %s\n" +
                "Auto-Escalated: %s\n\n" +
                "Immediate action is required. Review the ticket now:\n" +
                "%s\n\n" +
                "— TixManager Automated SLA Monitor",
                ticketId, ticketTitle, priority, dueAtFormatted, escalated ? "YES" : "NO", ticketUrl);

        String htmlBody = null;
        if (templateEngine != null) {
            try {
                Context context = new Context();
                context.setVariable("ticketId", ticketId);
                context.setVariable("ticketTitle", ticketTitle);
                context.setVariable("priority", priority);
                context.setVariable("dueAt", dueAtFormatted);
                context.setVariable("escalated", escalated);
                context.setVariable("ticketUrl", ticketUrl);
                htmlBody = templateEngine.process("email/sla-breach-email", context);
            } catch (Exception e) {
                log.warn("Failed to render sla-breach-email HTML template for ticket #{}: {}", ticketId, e.getMessage());
            }
        }

        dispatchNotification(toEmail, subject, textBody, htmlBody);
    }

    /**
     * Core dispatcher: sends MIME multipart via JavaMailSender SMTP relay if enabled, or logs operational audit event.
     */
    protected void dispatchNotification(String toEmail, String subject, String textBody, String htmlBody) {
        if (mailEnabled && mailSender != null) {
            try {
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, StandardCharsets.UTF_8.name());

                helper.setFrom(mailFrom);
                helper.setTo(toEmail);
                helper.setSubject(subject);

                if (htmlBody != null && !htmlBody.isBlank()) {
                    helper.setText(textBody, htmlBody);
                } else {
                    helper.setText(textBody, false);
                }

                mailSender.send(mimeMessage);
                log.info("Sent email via SMTP relay to <{}> [Subject: '{}']", toEmail, subject);
            } catch (MessagingException | MailException ex) {
                log.error("Failed to transmit email via SMTP relay to <{}> [Subject: '{}']: {}", toEmail, subject, ex.getMessage(), ex);
            }
        } else {
            log.info("[SIMULATED EMAIL NOTIFICATION] From: {} | To: {} | Subject: '{}' | HTML: {}\n---\n{}\n---",
                    mailFrom, toEmail, subject, (htmlBody != null ? "YES" : "NO"), textBody);
        }
    }
}
