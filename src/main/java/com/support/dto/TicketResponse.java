package com.support.dto;

import com.support.entity.TicketPriority;
import com.support.entity.TicketStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ==============================================================================================
 * RESPONSE DTO: TicketResponse
 * ==============================================================================================
 * 
 * Outbound response representation of a support ticket returned by REST endpoints.
 * Contains server-managed audit fields, relational display names, and lifecycle state.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Outbound representation of a support ticket")
public class TicketResponse {

    @Schema(description = "Unique ticket identifier", example = "101")
    private Long id;

    @Schema(description = "Ticket title summary", example = "Database connection timeout in production")
    private String title;

    @Schema(description = "Detailed issue description", example = "Application cannot connect to database.")
    private String description;

    @Schema(description = "Current ticket lifecycle status", example = "OPEN")
    private TicketStatus status;

    @Schema(description = "Assigned priority level", example = "HIGH")
    private TicketPriority priority;

    @Schema(description = "ID of the customer who created the ticket", example = "5")
    private Long customerId;

    @Schema(description = "Username of the customer", example = "farhan_dev")
    private String customerUsername;

    @Schema(description = "ID of assigned support agent (if assigned)", example = "3")
    private Long assignedAgentId;

    @Schema(description = "Username of assigned support agent (if assigned)", example = "priya_agent")
    private String assignedAgentName;

    @Schema(description = "Timestamp when ticket was submitted")
    private LocalDateTime createdAt;

    @Schema(description = "Timestamp when ticket was last updated")
    private LocalDateTime updatedAt;

    @Schema(description = "Username who created the ticket", example = "farhan_dev")
    private String createdBy;

    @Schema(description = "Username who last modified the ticket", example = "priya_agent")
    private String lastModifiedBy;

    @Schema(description = "Soft delete status flag")
    private boolean deleted;

    @Schema(description = "Audit timeline history of state transitions")
    private List<TicketStatusHistoryDTO> history;

    @Schema(description = "SLA resolution deadline timestamp")
    private LocalDateTime slaDueAt;

    @Schema(description = "Flag indicating if SLA deadline has been breached")
    private boolean slaBreached;

    @Schema(description = "Flag indicating if priority was escalated by the automated SLA engine")
    private boolean escalated;

    @Schema(description = "Timestamp when ticket was resolved (freezes SLA measurement)")
    private LocalDateTime resolvedAt;

    @Schema(description = "Dynamic SLA operational status: OK, WARNING, BREACHED, RESOLVED_MET, RESOLVED_BREACHED", example = "OK")
    private String slaStatus;

    @Schema(description = "Seconds remaining until SLA breach (negative if breached)", example = "3600")
    private Long remainingSeconds;
}
