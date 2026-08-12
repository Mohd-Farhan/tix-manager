package com.support.controller;

import com.support.dto.MessageDTO;
import com.support.dto.TicketDTO;
import com.support.dto.TicketStatusHistoryDTO;
import com.support.entity.TicketStatus;
import com.support.util.SecurityUtils;
import com.support.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@EnableMethodSecurity
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TicketDTO> createTicket(@Valid @RequestBody TicketDTO ticketDTO) {
        TicketDTO createTicket = ticketService.createTicket(ticketDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createTicket);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TicketDTO>> getTicketsForUser(@PathVariable Long userId) {
        List<TicketDTO> ticketDTOs = ticketService.getTicketsForUser(userId);
        return ResponseEntity.status(HttpStatus.OK).body(ticketDTOs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDTO> getTicketById(@PathVariable Long id) {
        TicketDTO ticketDTO = ticketService.getTicketById(id);
        return ResponseEntity.status(HttpStatus.OK).body(ticketDTO);
    }

    @PutMapping("/{ticketId}/assign")
    @PreAuthorize("hasRole('SUPPORT_AGENT') or hasRole('ADMIN')")
    public ResponseEntity<TicketDTO> assignTicket(@PathVariable Long ticketId, @RequestParam Long agentId) {
        TicketDTO ticketDTO = ticketService.assignTicket(ticketId, agentId);
        return ResponseEntity.status(HttpStatus.OK).body(ticketDTO);
    }

    @PutMapping("/{ticketId}/status")
    @PreAuthorize("hasRole('SUPPORT_AGENT') or hasRole('ADMIN')")
    public ResponseEntity<TicketDTO> updateTicketStatus(@PathVariable Long ticketId,
            @RequestParam TicketStatus status, Authentication authentication) {
        Long changedByUserId = securityUtils.resolveUserId(authentication);
        TicketDTO ticketDTO = ticketService.updateTicketStatus(ticketId, status, changedByUserId);
        return ResponseEntity.status(HttpStatus.OK).body(ticketDTO);
    }

    @PostMapping("/{id}/messages")
    public ResponseEntity<MessageDTO> addMessage(@PathVariable Long id, @Valid @RequestBody MessageDTO messageDTO,
            Authentication authentication) {

        Long senderId = securityUtils.resolveUserId(authentication);
        MessageDTO saved = ticketService.addMessage(id, senderId, messageDTO.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping("/{ticketId}/messages")
    public ResponseEntity<List<MessageDTO>> getMessageThread(@PathVariable Long ticketId) {
        List<MessageDTO> messageDTOs = ticketService.getTicketThread(ticketId);
        return ResponseEntity.status(HttpStatus.OK).body(messageDTOs);
    }

    @GetMapping("/{ticketId}/history")
    public ResponseEntity<List<TicketStatusHistoryDTO>> getTicketStatusHistory(@PathVariable Long ticketId) {
        List<TicketStatusHistoryDTO> ticketStatusHistoryDTOs = ticketService.getTicketStatusHistory(ticketId);
        return ResponseEntity.status(HttpStatus.OK).body(ticketStatusHistoryDTOs);
    }

    @DeleteMapping("/{ticketId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> softDeleteTicket(@PathVariable Long ticketId) {
        ticketService.softDeleteTicket(ticketId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TicketDTO>> getAllTicketsIncludingDeleted() {
        List<TicketDTO> ticketDTOs = ticketService.getAllTicketsIncludingDeleted();
        return ResponseEntity.status(HttpStatus.OK).body(ticketDTOs);
    }
}
