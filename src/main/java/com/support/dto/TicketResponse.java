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

    @Schema(description = "Soft delete status flag")
    private boolean deleted;

    @Schema(description = "Audit timeline history of state transitions")
    private List<TicketStatusHistoryDTO> history;
}
