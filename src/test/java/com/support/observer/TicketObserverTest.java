package com.support.observer;

import com.support.dto.NotificationResponse;
import com.support.entity.*;
import com.support.mapper.NotificationMapper;
import com.support.observer.event.TicketAssignedEvent;
import com.support.observer.event.TicketMessageAddedEvent;
import com.support.observer.event.TicketStatusChangedEvent;
import com.support.observer.listener.EmailNotificationObserver;
import com.support.observer.listener.InAppNotificationObserver;
import com.support.observer.publisher.TicketEventPublisher;
import com.support.repository.NotificationRepository;
import com.support.service.EmailService;
import com.support.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ==============================================================================================
 * AUTOMATED TESTING SUITE: Observer Pattern Tests (Events, Subject & Observers)
 * ==============================================================================================
 *
 * Demonstrates how the GoF Observer Pattern decouples domain event broadcasting from
 * email dispatching and in-app alert persistence.
 */
@ExtendWith(MockitoExtension.class)
class TicketObserverTest {

    private User customer;
    private User agent;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1L);
        customer.setUsername("customer_dan");
        customer.setEmail("dan@example.com");
        customer.setRole(UserRole.CUSTOMER);

        agent = new User();
        agent.setId(2L);
        agent.setUsername("agent_priya");
        agent.setEmail("priya@example.com");
        agent.setRole(UserRole.SUPPORT_AGENT);

        ticket = new Ticket();
        ticket.setId(42L);
        ticket.setTitle("Payment Timeout Error");
        ticket.setCustomer(customer);
        ticket.setAssignedAgent(agent);
        ticket.setStatus(TicketStatus.OPEN);
    }

    @Nested
    @DisplayName("Subject / Publisher Tests (TicketEventPublisher)")
    class PublisherTests {

        @Mock
        private ApplicationEventPublisher applicationEventPublisher;

        private TicketEventPublisher publisher;

        @BeforeEach
        void initPublisher() {
            publisher = new TicketEventPublisher(applicationEventPublisher);
        }

        @Test
        @DisplayName("publishTicketAssigned — Broadcasts TicketAssignedEvent to Spring Event Bus")
        void testPublishTicketAssigned() {
            publisher.publishTicketAssigned(ticket, agent, "admin");

            ArgumentCaptor<TicketAssignedEvent> captor = ArgumentCaptor.forClass(TicketAssignedEvent.class);
            verify(applicationEventPublisher).publishEvent(captor.capture());

            TicketAssignedEvent event = captor.getValue();
            assertThat(event.getTicketId()).isEqualTo(42L);
            assertThat(event.getAgent()).isEqualTo(agent);
            assertThat(event.getAssignedBy()).isEqualTo("admin");
        }

        @Test
        @DisplayName("publishTicketStatusChanged — Broadcasts TicketStatusChangedEvent")
        void testPublishTicketStatusChanged() {
            publisher.publishTicketStatusChanged(ticket, TicketStatus.OPEN, TicketStatus.IN_PROGRESS, agent);

            ArgumentCaptor<TicketStatusChangedEvent> captor = ArgumentCaptor.forClass(TicketStatusChangedEvent.class);
            verify(applicationEventPublisher).publishEvent(captor.capture());

            TicketStatusChangedEvent event = captor.getValue();
            assertThat(event.getTicketId()).isEqualTo(42L);
            assertThat(event.getOldStatus()).isEqualTo(TicketStatus.OPEN);
            assertThat(event.getNewStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("Observer 1 Tests: EmailNotificationObserver")
    class EmailObserverTests {

        @Mock
        private EmailService emailService;

        private EmailNotificationObserver observer;

        @BeforeEach
        void initObserver() {
            observer = new EmailNotificationObserver(emailService);
        }

        @Test
        @DisplayName("onTicketAssigned — Dispatches assignment notification email to agent")
        void testOnTicketAssigned() {
            TicketAssignedEvent event = new TicketAssignedEvent(42L, "Payment Timeout Error", agent, "admin");

            observer.onTicketAssigned(event);

            verify(emailService).sendTicketAssignedEmail("priya@example.com", 42L, "Payment Timeout Error", "agent_priya");
        }

        @Test
        @DisplayName("onTicketStatusChanged — Dispatches status change emails to customer and agent")
        void testOnTicketStatusChanged() {
            User admin = new User();
            admin.setId(99L);
            admin.setUsername("sysadmin");

            TicketStatusChangedEvent event = new TicketStatusChangedEvent(
                    42L, "Payment Timeout Error", TicketStatus.OPEN, TicketStatus.IN_PROGRESS, customer, agent, admin
            );

            observer.onTicketStatusChanged(event);

            verify(emailService).sendTicketStatusChangedEmail("dan@example.com", 42L, "Payment Timeout Error", TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
            verify(emailService).sendTicketStatusChangedEmail("priya@example.com", 42L, "Payment Timeout Error", TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Fault Tolerance — Exception during email dispatch is safely caught and logged")
        void testEmailDispatch_FaultTolerance() {
            doThrow(new RuntimeException("SMTP Server Unreachable"))
                    .when(emailService).sendTicketAssignedEmail(any(), any(), any(), any());

            TicketAssignedEvent event = new TicketAssignedEvent(42L, "Payment Timeout Error", agent, "admin");

            // Should not throw or crash
            observer.onTicketAssigned(event);

            verify(emailService).sendTicketAssignedEmail("priya@example.com", 42L, "Payment Timeout Error", "agent_priya");
        }
    }

    @Nested
    @DisplayName("Observer 2 Tests: InAppNotificationObserver")
    class InAppObserverTests {

        @Mock
        private NotificationRepository notificationRepository;

        private InAppNotificationObserver observer;

        @BeforeEach
        void initObserver() {
            observer = new InAppNotificationObserver(notificationRepository);
        }

        @Test
        @DisplayName("onTicketAssigned — Creates and saves in-app Notification for assigned agent")
        void testOnTicketAssigned() {
            TicketAssignedEvent event = new TicketAssignedEvent(42L, "Payment Timeout Error", agent, "admin");

            observer.onTicketAssigned(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());

            Notification saved = captor.getValue();
            assertThat(saved.getRecipient()).isEqualTo(agent);
            assertThat(saved.getTitle()).contains("Ticket Assigned: #42");
            assertThat(saved.getType()).isEqualTo(NotificationType.TICKET_ASSIGNED);
            assertThat(saved.isRead()).isFalse();
        }

        @Test
        @DisplayName("onTicketMessageAdded — Creates in-app Notification for reply recipient")
        void testOnTicketMessageAdded() {
            Message msg = new Message();
            msg.setId(10L);
            msg.setContent("Can you check the gateway response payload?");

            TicketMessageAddedEvent event = new TicketMessageAddedEvent(42L, "Payment Timeout Error", msg, agent, customer);

            observer.onTicketMessageAdded(event);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notificationRepository).save(captor.capture());

            Notification saved = captor.getValue();
            assertThat(saved.getRecipient()).isEqualTo(customer);
            assertThat(saved.getTitle()).contains("New Reply on Ticket #42");
            assertThat(saved.getType()).isEqualTo(NotificationType.NEW_MESSAGE);
        }
    }

    @Nested
    @DisplayName("In-App Notification Service Queries & Read Mutations")
    class NotificationServiceTests {

        @Mock
        private NotificationRepository notificationRepository;

        @Mock
        private NotificationMapper notificationMapper;

        private NotificationService service;

        @BeforeEach
        void initService() {
            service = new NotificationService(notificationRepository, notificationMapper);
        }

        @Test
        @DisplayName("getUnreadCount — Returns unread notification count for user")
        void testGetUnreadCount() {
            when(notificationRepository.countByRecipientIdAndReadFalse(1L)).thenReturn(4L);

            long count = service.getUnreadCount(1L);

            assertThat(count).isEqualTo(4L);
        }

        @Test
        @DisplayName("markAsRead — Sets read=true on existing notification")
        void testMarkAsRead() {
            Notification notif = Notification.builder()
                    .id(100L)
                    .recipient(customer)
                    .read(false)
                    .build();

            when(notificationRepository.findById(100L)).thenReturn(Optional.of(notif));
            when(notificationRepository.save(notif)).thenReturn(notif);
            when(notificationMapper.toResponse(notif)).thenReturn(NotificationResponse.builder().id(100L).read(true).build());

            NotificationResponse resp = service.markAsRead(100L, 1L);

            assertThat(resp.isRead()).isTrue();
            assertThat(notif.isRead()).isTrue();
            verify(notificationRepository).save(notif);
        }
    }
}
