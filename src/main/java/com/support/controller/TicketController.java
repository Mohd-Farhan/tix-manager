package com.support.controller;

import com.support.dto.CreateMessageRequest;
import com.support.dto.CreateTicketRequest;
import com.support.dto.MessageResponse;
import com.support.dto.TicketResponse;
import com.support.dto.TicketStatusHistoryDTO;
import com.support.entity.TicketStatus;
import com.support.service.TicketService;
import com.support.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ==============================================================================================
 * REST CONTROLLER: TicketController
 * ==============================================================================================
 * 
 * WHY THIS API DESIGN (Enterprise Best Practices):
 * 1. Request/Response DTO Separation: 
 *    - Uses `CreateTicketRequest` and `CreateMessageRequest` for incoming payloads to prevent
 *      mass-assignment security vulnerabilities (clients cannot forge `id`, `status`, `createdAt`).
 *    - Returns `TicketResponse` / `MessageResponse` for full outbound representation.
 * 2. Fine-Grained Authorization:
 *    - Role-based access control (RBAC) with `@PreAuthorize("hasRole('...')")`.
 *    - Object-level access control with custom SpEL evaluator `@PreAuthorize("@ticketSecurity.canAccessTicket(...)")`.
 * 3. OpenAPI / Swagger Documentation: Fully annotated with summaries and response codes for interactive API discovery.
 */
@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Endpoints for ticket creation, triage, messaging, and status transitions")
public class TicketController {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private SecurityUtils securityUtils;

    @Operation(summary = "Create a new support ticket", description = "Submits a new ticket for the authenticated customer. Customer ID is securely derived from JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid payload or validation failed"),
            @ApiResponse(responseCode = "401", description = "Unauthorized: Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Forbidden: User does not have CUSTOMER role")
    })
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            Authentication authentication) {
        Long customerId = securityUtils.resolveUserId(authentication);
        TicketResponse createdTicket = ticketService.createTicket(request, customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTicket);
    }

    @Operation(summary = "Get all active tickets", description = "Retrieves active non-deleted tickets across the organization for agents/admins.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of active tickets retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires SUPPORT_AGENT or ADMIN role")
    })
    @GetMapping
    @PreAuthorize("hasRole('SUPPORT_AGENT') or hasRole('ADMIN')")
    public ResponseEntity<List<TicketResponse>> getAllTickets() {
        List<TicketResponse> tickets = ticketService.getAllActiveTickets();
        return ResponseEntity.status(HttpStatus.OK).body(tickets);
    }

    @Operation(summary = "Get tickets for a specific user", description = "Retrieves tickets owned by or assigned to a user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User tickets retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Cannot view another user's tickets")
    })
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('SUPPORT_AGENT') or hasRole('ADMIN') or #userId == authentication.principal.id")
    public ResponseEntity<List<TicketResponse>> getTicketsForUser(@PathVariable Long userId) {
        List<TicketResponse> tickets = ticketService.getTicketsForUser(userId);
        return ResponseEntity.status(HttpStatus.OK).body(tickets);
    }

    // NOTE: Ticket ownership & access control is enforced via custom SpEL evaluator bean.
    // Annotation alternatives:
    // 1. @PreAuthorize("@ticketSecurity.canAccessTicket(#id, authentication)") [Applied below]
    // 2. @PostAuthorize("hasRole('ADMIN') or hasRole('SUPPORT_AGENT') or returnObject.body.customerId == authentication.principal.id")
    @Operation(summary = "Get ticket by ID", description = "Fetches details of a single ticket. Access restricted to creator, assigned agent, or admin.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket details retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Insufficient permissions to view this ticket"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @GetMapping("/{id}")
    @PreAuthorize("@ticketSecurity.canAccessTicket(#id, authentication)")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long id) {
        TicketResponse ticket = ticketService.getTicketById(id);
        return ResponseEntity.status(HttpStatus.OK).body(ticket);
    }

    @Operation(summary = "Assign ticket to support agent", description = "Assigns an open ticket to a specified support agent and transitions status to IN_PROGRESS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ticket assigned successfully"),
            @ApiResponse(responseCode = "400", description = "Assigned user is not a support agent"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires SUPPORT_AGENT or ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Ticket or agent not found")
    })
    @PutMapping("/{ticketId}/assign")
    @PreAuthorize("hasRole('SUPPORT_AGENT') or hasRole('ADMIN')")
    public ResponseEntity<TicketResponse> assignTicket(
            @PathVariable Long ticketId,
            @RequestParam Long agentId) {
        TicketResponse ticket = ticketService.assignTicket(ticketId, agentId);
        return ResponseEntity.status(HttpStatus.OK).body(ticket);
    }

    @Operation(summary = "Update ticket status", description = "Transitions ticket status (OPEN -> IN_PROGRESS -> RESOLVED) and logs audit history.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires SUPPORT_AGENT or ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @PutMapping("/{ticketId}/status")
    @PreAuthorize("hasRole('SUPPORT_AGENT') or hasRole('ADMIN')")
    public ResponseEntity<TicketResponse> updateTicketStatus(
            @PathVariable Long ticketId,
            @RequestParam TicketStatus status,
            Authentication authentication) {
        Long changedByUserId = securityUtils.resolveUserId(authentication);
        TicketResponse ticket = ticketService.updateTicketStatus(ticketId, status, changedByUserId);
        return ResponseEntity.status(HttpStatus.OK).body(ticket);
    }

    @Operation(summary = "Post message to ticket thread", description = "Appends a new message to the ticket conversation. Sender ID resolved from JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Message posted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Cannot post to a ticket you do not have access to"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @PostMapping("/{id}/messages")
    @PreAuthorize("@ticketSecurity.canAccessTicket(#id, authentication)")
    public ResponseEntity<MessageResponse> addMessage(
            @PathVariable Long id,
            @Valid @RequestBody CreateMessageRequest request,
            Authentication authentication) {
        Long senderId = securityUtils.resolveUserId(authentication);
        MessageResponse saved = ticketService.addMessage(id, senderId, request.getContent());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Get ticket message thread", description = "Retrieves all conversation messages in chronological order.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Message thread retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Access denied"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @GetMapping("/{ticketId}/messages")
    @PreAuthorize("@ticketSecurity.canAccessTicket(#ticketId, authentication)")
    public ResponseEntity<List<MessageResponse>> getMessageThread(@PathVariable Long ticketId) {
        List<MessageResponse> messages = ticketService.getTicketThread(ticketId);
        return ResponseEntity.status(HttpStatus.OK).body(messages);
    }

    @Operation(summary = "Get ticket status transition history", description = "Returns full audit timeline of state changes with actor timestamps.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit timeline retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Access denied"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @GetMapping("/{ticketId}/history")
    @PreAuthorize("@ticketSecurity.canAccessTicket(#ticketId, authentication)")
    public ResponseEntity<List<TicketStatusHistoryDTO>> getTicketStatusHistory(@PathVariable Long ticketId) {
        List<TicketStatusHistoryDTO> history = ticketService.getTicketStatusHistory(ticketId);
        return ResponseEntity.status(HttpStatus.OK).body(history);
    }

    @Operation(summary = "Soft delete ticket", description = "Flags a ticket as deleted without destroying audit history. Admin only.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Ticket soft-deleted"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @DeleteMapping("/{ticketId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> softDeleteTicket(@PathVariable Long ticketId) {
        ticketService.softDeleteTicket(ticketId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Get all tickets including soft-deleted", description = "Administrative endpoint for system auditing and compliance.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Complete tickets list retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires ADMIN role")
    })
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TicketResponse>> getAllTicketsIncludingDeleted() {
        List<TicketResponse> tickets = ticketService.getAllTicketsIncludingDeleted();
        return ResponseEntity.status(HttpStatus.OK).body(tickets);
    }
}
