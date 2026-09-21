package com.support.service;

import com.support.dto.CreateTicketRequest;
import com.support.dto.MessageResponse;
import com.support.dto.TicketResponse;
import com.support.entity.Message;
import com.support.entity.Ticket;
import com.support.entity.TicketPriority;
import com.support.entity.TicketStatus;
import com.support.entity.TicketStatusHistory;
import com.support.entity.User;
import com.support.entity.UserRole;
import com.support.mapper.MessageMapper;
import com.support.mapper.TicketMapper;
import com.support.mapper.TicketStatusHistoryMapper;
import com.support.repository.MessageRepository;
import com.support.repository.TicketRepository;
import com.support.repository.TicketStatusHistoryRepository;
import com.support.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ==============================================================================================
 * AUTOMATED TESTING SUITE: Unit Tests for TicketService
 * ==============================================================================================
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketStatusHistoryRepository historyRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketStatusHistoryMapper historyMapper;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private AuditService auditService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private TicketService ticketService;

    private User customer;
    private User agent;
    private Ticket ticket;
    private CreateTicketRequest createRequest;
    private TicketResponse ticketResponse;

    @BeforeEach
    void setUp() {
        customer = new User();
        customer.setId(1L);
        customer.setUsername("farhan_dev");
        customer.setEmail("farhan@example.com");
        customer.setRole(UserRole.CUSTOMER);

        agent = new User();
        agent.setId(2L);
        agent.setUsername("priya_agent");
        agent.setEmail("priya@tixmanager.com");
        agent.setRole(UserRole.SUPPORT_AGENT);

        ticket = new Ticket();
        ticket.setId(100L);
        ticket.setTitle("Database connection timeout");
        ticket.setDescription("Application logs display JDBC connection timeout errors.");
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(TicketPriority.HIGH);
        ticket.setCustomer(customer);
        ticket.setCreatedAt(LocalDateTime.now());

        createRequest = CreateTicketRequest.builder()
                .title("Database connection timeout")
                .description("Application logs display JDBC connection timeout errors.")
                .priority(TicketPriority.HIGH)
                .build();

        ticketResponse = TicketResponse.builder()
                .id(100L)
                .title("Database connection timeout")
                .description("Application logs display JDBC connection timeout errors.")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.HIGH)
                .customerId(1L)
                .customerUsername("farhan_dev")
                .build();
    }

    /**
     * TEST CASE 1: Creating a ticket should set status to OPEN and log initial history entry.
     */
    @Test
    @DisplayName("createTicket — Successfully create ticket, assign customer, and log audit history")
    void testCreateTicket_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(ticketMapper.toEntity(createRequest)).thenReturn(ticket);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(historyRepository.save(any(TicketStatusHistory.class))).thenReturn(new TicketStatusHistory());
        when(ticketMapper.toResponse(ticket)).thenReturn(ticketResponse);

        // Act
        TicketResponse created = ticketService.createTicket(createRequest, 1L);

        // Assert
        assertThat(created).isNotNull();
        assertThat(created.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(created.getTitle()).isEqualTo("Database connection timeout");

        // Verify repository interactions
        verify(ticketRepository, times(1)).save(ticket);
        verify(historyRepository, times(1)).save(any(TicketStatusHistory.class));
    }

    /**
     * TEST CASE 2: Assigning an agent to an OPEN ticket should update status to IN_PROGRESS.
     */
    @Test
    @DisplayName("assignTicket — Successfully assign agent and transition status to IN_PROGRESS")
    void testAssignTicket_Success() {
        // Arrange
        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(2L)).thenReturn(Optional.of(agent));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(historyRepository.save(any(TicketStatusHistory.class))).thenReturn(new TicketStatusHistory());

        TicketResponse inProgressResponse = TicketResponse.builder()
                .id(100L)
                .status(TicketStatus.IN_PROGRESS)
                .assignedAgentId(2L)
                .build();
        when(ticketMapper.toResponse(ticket)).thenReturn(inProgressResponse);

        // Act
        TicketResponse assigned = ticketService.assignTicket(100L, 2L);

        // Assert
        assertThat(assigned).isNotNull();
        assertThat(assigned.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getAssignedAgent()).isEqualTo(agent);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        verify(historyRepository, times(1)).save(any(TicketStatusHistory.class));
    }

    /**
     * TEST CASE 3: Assigning a user without SUPPORT_AGENT role must throw InvalidOperationException.
     */
    @Test
    @DisplayName("assignTicket — Throw InvalidOperationException when assigned user is not an agent")
    void testAssignTicket_InvalidRole_ThrowsException() {
        // Arrange: User is CUSTOMER instead of SUPPORT_AGENT
        User invalidAgent = new User();
        invalidAgent.setId(3L);
        invalidAgent.setRole(UserRole.CUSTOMER);

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(3L)).thenReturn(Optional.of(invalidAgent));

        // Act & Assert
        assertThatThrownBy(() -> ticketService.assignTicket(100L, 3L))
                .isInstanceOf(com.support.exception.InvalidOperationException.class)
                .hasMessageContaining("User is not an agent");
        verify(ticketRepository, never()).save(any(Ticket.class));
    }

    /**
     * TEST CASE 4: Updating status transitions ticket and logs previous/new status in history.
     */
    @Test
    @DisplayName("updateTicketStatus — Transition status to RESOLVED and record audit log")
    void testUpdateTicketStatus_Success() {
        // Arrange
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(2L)).thenReturn(Optional.of(agent));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        TicketResponse resolvedResponse = TicketResponse.builder()
                .id(100L)
                .status(TicketStatus.RESOLVED)
                .build();
        when(ticketMapper.toResponse(ticket)).thenReturn(resolvedResponse);

        // Act
        TicketResponse result = ticketService.updateTicketStatus(100L, TicketStatus.RESOLVED, 2L);

        // Assert
        assertThat(result.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        verify(historyRepository, times(1)).save(any(TicketStatusHistory.class));
    }

    /**
     * TEST CASE 5: Adding a message to a ticket conversation thread.
     */
    @Test
    @DisplayName("addMessage — Save message and return mapped response")
    void testAddMessage_Success() {
        // Arrange
        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse expectedResponse = MessageResponse.builder()
                .id(50L)
                .ticketId(100L)
                .senderId(1L)
                .content("I am still experiencing the timeout issue.")
                .build();
        when(messageMapper.toResponse(any(Message.class))).thenReturn(expectedResponse);

        // Act
        MessageResponse result = ticketService.addMessage(100L, 1L, "I am still experiencing the timeout issue.");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEqualTo("I am still experiencing the timeout issue.");
        verify(messageRepository, times(1)).save(any(Message.class));
    }

    /**
     * TEST CASE 6: Soft delete marks ticket deleted=true without removing data.
     */
    @Test
    @DisplayName("softDeleteTicket — Flag ticket as deleted")
    void testSoftDeleteTicket_Success() {
        // Arrange
        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));

        // Act
        ticketService.softDeleteTicket(100L);

        // Assert
        assertThat(ticket.isDeleted()).isTrue();
        verify(ticketRepository, times(1)).save(ticket);
    }
}
