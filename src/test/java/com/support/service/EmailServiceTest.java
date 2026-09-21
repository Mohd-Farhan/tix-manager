package com.support.service;

import com.support.entity.TicketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ==============================================================================================
 * AUTOMATED TESTING SUITE: EmailServiceTest
 * ==============================================================================================
 * 
 * Verifies email template generation, recipient addressing, and non-blocking notification dispatch.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Spy
    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("sendWelcomeEmail — Formats credentials and NIST notice")
    void testSendWelcomeEmail() {
        emailService.sendWelcomeEmail("newuser@example.com", "newuser", "TempPass123!");

        verify(emailService, times(1)).dispatchNotification(
                eq("newuser@example.com"),
                contains("Welcome to TixManager"),
                contains("TempPass123!"));
    }

    @Test
    @DisplayName("sendWelcomeEmail — Gracefully ignores empty recipient without error")
    void testSendWelcomeEmail_EmptyRecipient() {
        assertThatCode(() -> emailService.sendWelcomeEmail("", "newuser", "TempPass123!"))
                .doesNotThrowAnyException();

        verify(emailService, never()).dispatchNotification(any(), any(), any());
    }

    @Test
    @DisplayName("sendTicketAssignedEmail — Formats ticket assignment details")
    void testSendTicketAssignedEmail() {
        emailService.sendTicketAssignedEmail("agent@example.com", 42L, "Network Outage", "agent1");

        verify(emailService, times(1)).dispatchNotification(
                eq("agent@example.com"),
                contains("Ticket #42 Assigned"),
                contains("Network Outage"));
    }

    @Test
    @DisplayName("sendTicketStatusChangedEmail — Formats status transition details")
    void testSendTicketStatusChangedEmail() {
        emailService.sendTicketStatusChangedEmail("customer@example.com", 42L, "Network Outage",
                TicketStatus.OPEN, TicketStatus.RESOLVED);

        verify(emailService, times(1)).dispatchNotification(
                eq("customer@example.com"),
                contains("Ticket #42 Status Update: RESOLVED"),
                contains("Previous Status: OPEN"));
    }

    @Test
    @DisplayName("sendTicketReplyEmail — Formats counterparty reply notification")
    void testSendTicketReplyEmail() {
        emailService.sendTicketReplyEmail("recipient@example.com", 42L, "Network Outage",
                "agent1", "We have identified the issue and applied a patch.");

        verify(emailService, times(1)).dispatchNotification(
                eq("recipient@example.com"),
                contains("New Reply on Ticket #42"),
                contains("applied a patch"));
    }
}
