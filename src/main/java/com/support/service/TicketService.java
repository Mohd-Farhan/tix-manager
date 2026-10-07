package com.support.service;

import com.support.dto.CreateTicketRequest;
import com.support.dto.MessageResponse;
import com.support.dto.TicketResponse;
import com.support.dto.TicketStatusHistoryDTO;
import com.support.entity.*;
import com.support.exception.InvalidOperationException;
import com.support.exception.ResourceNotFoundException;
import com.support.mapper.MessageMapper;
import com.support.mapper.TicketMapper;
import com.support.mapper.TicketStatusHistoryMapper;
import com.support.observer.publisher.TicketEventPublisher;
import com.support.repository.*;
import com.support.state.TicketStateFactory;
import com.support.strategy.routing.RoutingStrategyType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ==============================================================================================
 * SERVICE: TicketService
 * ==============================================================================================
 * 
 * WHY TRANSACTIONAL BOUNDARIES & READ-ONLY OPTIMIZATIONS ARE APPLIED:
 * 
 * 1. Read-Only Transaction Boundaries (@Transactional(readOnly = true)):
 *    - Applied at class level. Informs Hibernate/JPA not to track dirty checks or take snapshot copies
 *      of retrieved entities, dramatically reducing CPU cycles and heap memory consumption.
 *    - Directs read queries to read-replica databases in distributed/clustered configurations.
 * 
 * 2. Explicit Write Transactions (@Transactional):
 *    - Applied strictly to mutation methods (`createTicket`, `assignTicket`, `updateTicketStatus`,
 *      `addMessage`, `softDeleteTicket`).
 *    - Guarantees ACID atomicity: if writing audit history fails, the ticket state change rolls back.
 * 
 * 3. Pagination & High-Volume Query Optimization:
 *    - Provides Pageable overloads to stream large ticket queues without memory exhaustion.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class TicketService {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketMapper ticketMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TicketStatusHistoryRepository ticketStatusHistoryRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private TicketStatusHistoryMapper ticketStatusHistoryMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private AuditService auditService;

    @Autowired
    private TicketEventPublisher ticketEventPublisher;

    @Autowired
    private SlaService slaService;

    @Autowired
    private TicketStateFactory ticketStateFactory;

    @Autowired
    private TicketRoutingService ticketRoutingService;

    /**
     * MUTATION: Create a new support ticket and record initial audit history.
     */
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, Long customerId) {
        User customer = userRepository.findById(customerId).orElseThrow(
                () -> new ResourceNotFoundException("User", "id", customerId));

        Ticket ticket = ticketMapper.toEntity(request);
        ticket.setCustomer(customer);
        ticket.setStatus(TicketStatus.OPEN);

        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        ticket.setSlaDueAt(slaService.calculateSlaDueAt(ticket.getPriority(), now));
        ticket.setSlaBreached(false);
        ticket.setEscalated(false);

        ticketRepository.save(ticket);
        auditService.recordEntityChange("TICKET", ticket.getId(), "CREATE", customer.getUsername(),
                "Ticket created: '" + ticket.getTitle() + "'");
        log.info("Created ticket id={} for customer id={}", ticket.getId(), customer.getId());

        // Audit History Entry
        TicketStatusHistory history = new TicketStatusHistory();
        history.setPreviousStatus(null);
        history.setNewStatus(TicketStatus.OPEN);
        history.setTicket(ticket);
        history.setChangedBy(customer);
        ticketStatusHistoryRepository.save(history);

        return ticketMapper.toResponse(ticket);
    }

    /**
     * QUERY: Fetch ticket by ID (Optimized with @EntityGraph to load customer & agent in 1 query).
     */
    public TicketResponse getTicketById(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
        return ticketMapper.toResponse(ticket);
    }

    /**
     * MUTATION: Assign ticket to support agent with concurrency check.
     */
    @Transactional
    public TicketResponse assignTicket(Long ticketId, Long agentId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        TicketStatus previousStatus = ticket.getStatus();

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", agentId));
        if (agent.getRole() != UserRole.SUPPORT_AGENT) {
            throw new InvalidOperationException("User is not an agent.");
        }
        // GOF STATE PATTERN: Delegate assignment logic to current state handler
        ticketStateFactory.applyAssignment(ticket, agent);
        ticketRepository.save(ticket);
        auditService.recordEntityChange("TICKET", ticket.getId(), "ASSIGN", agent.getUsername(),
                "Ticket assigned to agent '" + agent.getUsername() + "'");
        log.info("Ticket id={} assigned to agent id={}", ticketId, agentId);

        TicketStatusHistory history = new TicketStatusHistory();
        history.setNewStatus(ticket.getStatus());
        history.setPreviousStatus(previousStatus);
        history.setTicket(ticket);
        history.setChangedBy(agent);
        ticketStatusHistoryRepository.save(history);

        // GOF OBSERVER PATTERN: Publish assignment event to all subscribed observers (Email, In-App, Audit)
        ticketEventPublisher.publishTicketAssigned(ticket, agent, agent.getUsername());

        return ticketMapper.toResponse(ticket);
    }

    /**
     * MUTATION: Automatically assign a ticket using Strategy Pattern routing.
     * Selects an optimal agent based on workload, round-robin, or priority rules,
     * then executes state pattern assignment.
     */
    @Transactional
    public TicketResponse autoAssignTicket(Long ticketId, RoutingStrategyType strategyType) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        // Use Strategy Pattern to resolve the best agent
        User bestAgent = ticketRoutingService.resolveAgent(ticket, strategyType);

        log.info("Auto-assigning ticket id={} to agent id={} using strategy={}", ticketId, bestAgent.getId(), strategyType);

        // Delegate to assignTicket, which executes GoF State Pattern lifecycle transition
        return assignTicket(ticketId, bestAgent.getId());
    }

    /**
     * MUTATION: Automatically assign all unassigned open tickets using Strategy Pattern routing.
     * Evaluates all unassigned OPEN tickets and routes each to the optimal agent.
     */
    @Transactional
    public List<TicketResponse> autoAssignAllUnassigned(RoutingStrategyType strategyType) {
        List<Ticket> unassigned = ticketRepository.findByStatus(TicketStatus.OPEN)
                .stream()
                .filter(t -> t.getAssignedAgent() == null)
                .toList();

        if (unassigned.isEmpty()) {
            log.info("No unassigned OPEN tickets found for batch auto-assignment.");
            return List.of();
        }

        log.info("Batch auto-assigning {} unassigned OPEN tickets using strategy={}", unassigned.size(), strategyType);
        List<TicketResponse> assigned = new java.util.ArrayList<>();
        for (Ticket ticket : unassigned) {
            assigned.add(autoAssignTicket(ticket.getId(), strategyType));
        }
        return assigned;
    }

    /**
     * MUTATION: Update ticket status with audit history.
     */
    @Transactional
    public TicketResponse updateTicketStatus(Long ticketId, TicketStatus newStatus, Long changedByUserId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        User changedBy = userRepository.findById(changedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", changedByUserId));

        TicketStatus oldStatus = ticket.getStatus();

        // GOF STATE PATTERN: Delegate validation and state-specific business side effects (e.g. SLA)
        ticketStateFactory.applyTransition(ticket, newStatus, changedBy);

        ticketRepository.save(ticket);
        auditService.recordEntityChange("TICKET", ticket.getId(), "STATUS_CHANGE", changedBy.getUsername(),
                "Ticket status updated from " + oldStatus + " to " + newStatus);
        log.info("Ticket id={} status updated: {} -> {} by userId={}", ticketId, oldStatus, newStatus, changedByUserId);

        // Audit History Entry
        TicketStatusHistory history = new TicketStatusHistory();
        history.setNewStatus(newStatus);
        history.setPreviousStatus(oldStatus);
        history.setTicket(ticket);
        history.setChangedBy(changedBy);
        ticketStatusHistoryRepository.save(history);

        // GOF OBSERVER PATTERN: Publish status change event to all subscribed observers (Email, In-App, Audit)
        ticketEventPublisher.publishTicketStatusChanged(ticket, oldStatus, newStatus, changedBy);

        return ticketMapper.toResponse(ticket);
    }

    /**
     * MUTATION: Adjust ticket priority and recalculate SLA target deadline if active.
     */
    @Transactional
    public TicketResponse updatePriority(Long ticketId, TicketPriority newPriority, Long changedByUserId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        User changedBy = userRepository.findById(changedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", changedByUserId));

        TicketPriority oldPriority = ticket.getPriority();
        ticket.setPriority(newPriority);

        // Recalculate SLA due date from creation timestamp if ticket is still active
        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            java.time.LocalDateTime baseline = ticket.getCreatedAt() != null ? ticket.getCreatedAt() : java.time.LocalDateTime.now();
            java.time.LocalDateTime newDueAt = slaService.calculateSlaDueAt(newPriority, baseline);
            ticket.setSlaDueAt(newDueAt);
            ticket.setSlaBreached(java.time.LocalDateTime.now().isAfter(newDueAt));
        }

        ticketRepository.save(ticket);
        auditService.recordEntityChange("TICKET", ticket.getId(), "PRIORITY_CHANGE", changedBy.getUsername(),
                "Priority updated from " + oldPriority + " to " + newPriority);
        log.info("Ticket id={} priority updated: {} -> {} by userId={}", ticketId, oldPriority, newPriority, changedByUserId);

        return ticketMapper.toResponse(ticket);
    }

    /**
     * MUTATION: Append conversation message to ticket thread.
     */
    @Transactional
    public MessageResponse addMessage(Long ticketId, Long senderId, String content) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(
                () -> new ResourceNotFoundException("Ticket", "id", ticketId));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", senderId));

        Message message = new Message();
        message.setTicket(ticket);
        message.setSender(sender);
        message.setContent(content);
        messageRepository.save(message);
        auditService.recordEntityChange("MESSAGE", message.getId(), "CREATE", sender.getUsername(),
                "Added message to ticket #" + ticketId);
        log.info("Added message id={} to ticket id={} by sender id={}", message.getId(), ticketId, senderId);

        // GOF OBSERVER PATTERN: Publish message reply event to all subscribed observers (Email, In-App, Audit)
        User recipient = (ticket.getCustomer() != null && senderId.equals(ticket.getCustomer().getId()))
                ? ticket.getAssignedAgent()
                : ticket.getCustomer();
        ticketEventPublisher.publishTicketMessageAdded(ticket, message, sender, recipient);

        return messageMapper.toResponse(message);
    }

    /**
     * QUERY: List-based retrieval for user tickets.
     */
    public List<TicketResponse> getTicketsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        switch (user.getRole()) {
            case UserRole.CUSTOMER:
                return ticketMapper.toResponseList(ticketRepository.findByCustomerId(userId));
            case UserRole.SUPPORT_AGENT:
                return ticketMapper.toResponseList(ticketRepository.findByAssignedAgentId(userId));
            case UserRole.ADMIN:
                return ticketMapper.toResponseList(ticketRepository.findAll());
            default:
                throw new InvalidOperationException("Invalid user role: " + user.getRole());
        }
    }

    /**
     * QUERY: Paginated retrieval for user tickets.
     */
    public Page<TicketResponse> getTicketsForUser(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        switch (user.getRole()) {
            case UserRole.CUSTOMER:
                return ticketRepository.findByCustomerId(userId, pageable).map(ticketMapper::toResponse);
            case UserRole.SUPPORT_AGENT:
                return ticketRepository.findByAssignedAgentId(userId, pageable).map(ticketMapper::toResponse);
            case UserRole.ADMIN:
                return ticketRepository.findAll(pageable).map(ticketMapper::toResponse);
            default:
                throw new InvalidOperationException("Invalid user role: " + user.getRole());
        }
    }

    /**
     * QUERY: Fetch ticket message thread in chronological order.
     */
    public List<MessageResponse> getTicketThread(Long ticketId) {
        List<Message> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        return messageMapper.toResponseList(messages);
    }

    /**
     * QUERY: Fetch status audit history.
     */
    public List<TicketStatusHistoryDTO> getTicketStatusHistory(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
        List<TicketStatusHistory> histories = ticketStatusHistoryRepository
                .findByTicketIdOrderByChangedAtAsc(ticket.getId());
        return ticketStatusHistoryMapper.toDTOList(histories);
    }

    /**
     * MUTATION: Soft delete ticket without destroying relational integrity.
     */
    @Transactional
    public void softDeleteTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
        ticket.setDeleted(true);
        ticketRepository.save(ticket);
        auditService.recordEntityChange("TICKET", ticket.getId(), "DELETE", null,
                "Ticket #" + ticketId + " deactivated (soft-deleted)");
        log.warn("Ticket id={} soft-deleted", ticketId);
    }

    /**
     * QUERY: All active tickets list.
     */
    public List<TicketResponse> getAllActiveTickets() {
        List<Ticket> tickets = ticketRepository.findAll();
        return ticketMapper.toResponseList(tickets);
    }

    /**
     * QUERY: Paginated active tickets.
     */
    public Page<TicketResponse> getAllActiveTickets(Pageable pageable) {
        return ticketRepository.findAll(pageable).map(ticketMapper::toResponse);
    }

    /**
     * QUERY: All tickets including soft-deleted for administration.
     */
    public List<TicketResponse> getAllTicketsIncludingDeleted() {
        List<Ticket> tickets = ticketRepository.findAllIncludingDeleted();
        return ticketMapper.toResponseList(tickets);
    }

    /**
     * QUERY: Paginated active tickets with optional SLA status filter (BREACHED, WARNING, OK, ALL).
     */
    public Page<TicketResponse> getActiveTicketsFiltered(String slaStatus, Pageable pageable) {
        if (slaStatus == null || slaStatus.isBlank() || "ALL".equalsIgnoreCase(slaStatus)) {
            return ticketRepository.findAll(pageable).map(ticketMapper::toResponse);
        }

        List<TicketStatus> activeStatuses = List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();

        if ("BREACHED".equalsIgnoreCase(slaStatus)) {
            return ticketRepository.findByStatusInAndSlaBreachedTrue(activeStatuses, pageable).map(ticketMapper::toResponse);
        } else if ("WARNING".equalsIgnoreCase(slaStatus) || "NEAR_BREACH".equalsIgnoreCase(slaStatus)) {
            return ticketRepository.findByStatusInAndSlaBreachedFalseAndSlaDueAtBetween(activeStatuses, now, now.plusHours(2), pageable).map(ticketMapper::toResponse);
        } else if ("OK".equalsIgnoreCase(slaStatus) || "WITHIN_SLA".equalsIgnoreCase(slaStatus)) {
            return ticketRepository.findByStatusInAndSlaBreachedFalseAndSlaDueAtBetween(activeStatuses, now.plusHours(2), now.plusYears(10), pageable).map(ticketMapper::toResponse);
        }

        return ticketRepository.findAll(pageable).map(ticketMapper::toResponse);
    }
}
