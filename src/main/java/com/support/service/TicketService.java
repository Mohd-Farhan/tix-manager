package com.support.service;

import com.support.dto.MessageDTO;
import com.support.dto.TicketDTO;
import com.support.dto.TicketStatusHistoryDTO;
import com.support.entity.*;
import com.support.mapper.MessageMapper;
import com.support.mapper.TicketMapper;
import com.support.mapper.TicketStatusHistoryMapper;
import com.support.repository.*;

import jakarta.persistence.EntityNotFoundException;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public TicketDTO createTicket(TicketDTO ticketDTO) {
        Ticket ticket = ticketMapper.toEntity(ticketDTO);
        User customer = userRepository.findById(ticketDTO.getCustomerId()).orElseThrow(
                () -> new EntityNotFoundException("Customer not found with id : " + ticketDTO.getCustomerId() + "."));
        ticket.setCustomer(customer);
        ticket.setStatus(TicketStatus.OPEN);
        ticketRepository.save(ticket);

        // History
        TicketStatusHistory history = new TicketStatusHistory();
        history.setPreviousStatus(null);
        history.setNewStatus(TicketStatus.OPEN);
        history.setTicket(ticket);
        history.setChangedBy(customer);

        ticketStatusHistoryRepository.save(history);

        return ticketMapper.toDTO(ticket);
    }

    public TicketDTO getTicketById(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id : " + ticketId + "."));
        return ticketMapper.toDTO(ticket);
    }

    public TicketDTO assignTicket(Long ticketId, Long agentId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id : " + ticketId + "."));

        TicketStatus previoStatus = ticket.getStatus();

        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new EntityNotFoundException("Agent not found with id : " + agentId + "."));
        if (agent.getRole() != UserRole.SUPPORT_AGENT) {
            throw new IllegalArgumentException("User is not an agent.");
        }
        ticket.setAssignedAgent(agent);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.save(ticket);

        TicketStatusHistory history = new TicketStatusHistory();
        history.setNewStatus(TicketStatus.IN_PROGRESS);
        history.setPreviousStatus(previoStatus);
        history.setTicket(ticket);
        history.setChangedBy(agent);
        ticketStatusHistoryRepository.save(history);

        return ticketMapper.toDTO(ticket);
    }

    public TicketDTO updateTicketStatus(Long ticketId, TicketStatus newStatus, Long changedByUserId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id : " + ticketId + "."));

        User changedBy = userRepository.findById(changedByUserId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id : " + changedByUserId + "."));

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(newStatus);
        ticketRepository.save(ticket);

        // History
        TicketStatusHistory history = new TicketStatusHistory();
        history.setNewStatus(newStatus);
        history.setPreviousStatus(oldStatus);
        history.setTicket(ticket);
        history.setChangedBy(changedBy);
        ticketStatusHistoryRepository.save(history);

        return ticketMapper.toDTO(ticket);
    }

    public MessageDTO addMessage(Long ticketId, Long senderId, String content) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(
                () -> new EntityNotFoundException("Ticket not found with id : " + ticketId + "."));
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id : " + senderId + "."));
        Message message = new Message();
        message.setTicket(ticket);
        message.setSender(sender);
        message.setContent(content);
        messageRepository.save(message);
        return messageMapper.toDTO(message);
    }

    public List<TicketDTO> getTicketsForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id : " + userId + "."));

        switch (user.getRole()) {
            case UserRole.CUSTOMER:
                return ticketMapper.toDTOList(ticketRepository.findByCustomerId(userId));
            case UserRole.SUPPORT_AGENT:
                return ticketMapper.toDTOList(ticketRepository.findByAssignedAgentId(userId));
            case UserRole.ADMIN:
                return ticketMapper.toDTOList(ticketRepository.findAll());
            default:
                throw new IllegalArgumentException("Invalid user role.");
        }
    }

    public List<MessageDTO> getTicketThread(Long ticketId) {
        List<Message> messages = messageRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        return messageMapper.toDTOList(messages);
    }

    public List<TicketStatusHistoryDTO> getTicketStatusHistory(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id : " + ticketId + "."));
        List<TicketStatusHistory> histories = ticketStatusHistoryRepository
                .findByTicketIdOrderByChangedAtAsc(ticket.getId());
        return ticketStatusHistoryMapper.toDTOList(histories);
    }

    public void softDeleteTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found with id : " + ticketId + "."));
        ticket.setDeleted(true);
        ticketRepository.save(ticket);
    }

    public List<TicketDTO> getAllActiveTickets() {
        List<Ticket> tickets = ticketRepository.findAll();
        return ticketMapper.toDTOList(tickets);
    }

    public List<TicketDTO> getAllTicketsIncludingDeleted() {
        List<Ticket> tickets = ticketRepository.findAllIncludingDeleted();
        return ticketMapper.toDTOList(tickets);
    }

}
