package com.support.service;

import com.support.entity.TicketStatus;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * ==============================================================================================
 * AUTOMATED TESTING SUITE: EmailServiceTest
 * ==============================================================================================
 * 
 * Verifies email template generation, Thymeleaf HTML rendering, recipient addressing,
 * simulation logging mode, and real SMTP relay dispatch with JavaMailSender.
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Spy
    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        // Initialize real Thymeleaf template engine for testing actual HTML templates
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        ReflectionTestUtils.setField(emailService, "templateEngine", templateEngine);
        ReflectionTestUtils.setField(emailService, "mailFrom", "no-reply@tixmanager.com");
        ReflectionTestUtils.setField(emailService, "frontendBaseUrl", "http://localhost:4200");
    }

    @Test
    @DisplayName("sendWelcomeEmail — Renders credentials, NIST alert, and dispatches in simulation mode")
    void testSendWelcomeEmail_Simulation() {
        emailService.sendWelcomeEmail("newuser@example.com", "newuser", "TempPass123!");

        verify(emailService, times(1)).dispatchNotification(
                eq("newuser@example.com"),
                contains("Welcome to TixManager"),
                contains("TempPass123!"),
                contains("NIST SP 800-63B"));
    }

    @Test
    @DisplayName("sendWelcomeEmail — Gracefully ignores empty recipient without error")
    void testSendWelcomeEmail_EmptyRecipient() {
        assertThatCode(() -> emailService.sendWelcomeEmail("", "newuser", "TempPass123!"))
                .doesNotThrowAnyException();

        verify(emailService, never()).dispatchNotification(any(), any(), any(), any());
    }

    @Test
    @DisplayName("sendTicketAssignedEmail — Renders HTML template and dispatches assignment details")
    void testSendTicketAssignedEmail() {
        emailService.sendTicketAssignedEmail("agent@example.com", 42L, "Network Outage", "agent1");

        verify(emailService, times(1)).dispatchNotification(
                eq("agent@example.com"),
                contains("Ticket #42 Assigned"),
                contains("Network Outage"),
                contains("agent1"));
    }

    @Test
    @DisplayName("sendTicketStatusChangedEmail — Renders transition badges and dispatches status update")
    void testSendTicketStatusChangedEmail() {
        emailService.sendTicketStatusChangedEmail("customer@example.com", 42L, "Network Outage",
                TicketStatus.OPEN, TicketStatus.RESOLVED);

        verify(emailService, times(1)).dispatchNotification(
                eq("customer@example.com"),
                contains("Ticket #42 Status Update: RESOLVED"),
                contains("Previous Status: OPEN"),
                contains("RESOLVED"));
    }

    @Test
    @DisplayName("sendTicketReplyEmail — Renders message bubble and dispatches reply notification")
    void testSendTicketReplyEmail() {
        emailService.sendTicketReplyEmail("recipient@example.com", 42L, "Network Outage",
                "agent1", "We have identified the issue and applied a patch.");

        verify(emailService, times(1)).dispatchNotification(
                eq("recipient@example.com"),
                contains("New Reply on Ticket #42"),
                contains("applied a patch"),
                contains("agent1"));
    }

    @Test
    @DisplayName("dispatchNotification — Transmits MIME message via JavaMailSender when mailEnabled is true")
    void testDispatchNotification_RealSmtp() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);

        MimeMessage mockMime = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mockMime);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        emailService.sendWelcomeEmail("user@example.com", "user", "SecretPass!");

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("dispatchNotification — Gracefully catches MailSendException without rethrowing")
    void testDispatchNotification_MailExceptionResilience() {
        ReflectionTestUtils.setField(emailService, "mailEnabled", true);
        ReflectionTestUtils.setField(emailService, "mailSender", mailSender);

        MimeMessage mockMime = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mockMime);
        doThrow(new MailSendException("SMTP relay timeout")).when(mailSender).send(any(MimeMessage.class));

        assertThatCode(() -> emailService.sendWelcomeEmail("user@example.com", "user", "SecretPass!"))
                .doesNotThrowAnyException();

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
