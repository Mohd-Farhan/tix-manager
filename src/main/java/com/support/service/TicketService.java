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
import com.support.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
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

    public TicketResponse createTicket(CreateTicketRequest request, Long customerId) {
        User customer = userRepository.findById(customerId).orElseThrow(
                () -> new ResourceNotFoundException("User", "id", customerId));

        Ticket ticket = ticketMapper.toEntity(request);
        ticket.setCustomer(customer);
        ticket.setStatus(TicketStatus.OPEN);
        ticketRepository.save(ticket);

        // Audit History Entry
        TicketStatusHistory history = new TicketStatusHistory();
        history.setPreviousStatus(null);
        history.setNewStatus(TicketStatus.OPEN);
        history.setTicket(ticket);
        history.setChangedBy(customer);
        ticketStatusHistoryRepository.save(history);

        return ticketMapper.toResponse(ticket);
    }

    public TicketResponse getTicketById(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
        return ticketMapper.toResponse(ticket);
    }

    public TicketResponse assignTicket(Long ticketId, Long agentId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        TicketStatus previousStatus = ticket.getStatus();

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", agentId));
        if (agent.getRole() != UserRole.SUPPORT_AGENT) {
            throw new InvalidOperationException("User is not an agent.");
        }
        ticket.setAssignedAgent(agent);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.save(ticket);

        TicketStatusHistory history = new TicketStatusHistory();
        history.setNewStatus(TicketStatus.IN_PROGRESS);
        history.setPreviousStatus(previousStatus);
        history.setTicket(ticket);
        history.setChangedBy(agent);
        ticketStatusHistoryRepository.save(history);

        return ticketMapper.toResponse(ticket);
    }

    public TicketResponse updateTicketStatus(Long ticketId, TicketStatus newStatus, Long changedByUserId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        User changedBy = userRepository.findById(changedByUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", changedByUserId));

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(newStatus);
        ticketRepository.save(ticket);

        // Audit History Entry
        TicketStatusHistory history = new TicketStatusHistory();
        history.setNewStatus(newStatus);
        history.setPreviousStatus(oldStatus);
        history.setTicket(ticket);
        history.setChangedBy(changedBy);
        ticketStatusHistoryRepository.save(history);

        return ticketMapper.toResponse(ticket);
    }

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

        return messageMapper.toResponse(message);
    }

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

    public List<MessageResponse> getTicketThread(Long ticketId) {
        List<Message> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        return messageMapper.toResponseList(messages);
    }

    public List<TicketStatusHistoryDTO> getTicketStatusHistory(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
        List<TicketStatusHistory> histories = ticketStatusHistoryRepository
                .findByTicketIdOrderByChangedAtAsc(ticket.getId());
        return ticketStatusHistoryMapper.toDTOList(histories);
    }

    public void softDeleteTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));
        ticket.setDeleted(true);
        ticketRepository.save(ticket);
    }

    public List<TicketResponse> getAllActiveTickets() {
        List<Ticket> tickets = ticketRepository.findAll();
        return ticketMapper.toResponseList(tickets);
    }

    public List<TicketResponse> getAllTicketsIncludingDeleted() {
        List<Ticket> tickets = ticketRepository.findAllIncludingDeleted();
        return ticketMapper.toResponseList(tickets);
    }
}
