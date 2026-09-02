package com.support.dto;

import com.support.entity.TicketPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ==============================================================================================
 * REQUEST DTO: CreateTicketRequest
 * ==============================================================================================
 * 
 * WHY SEPARATE REQUEST DTO FROM RESPONSE DTO (Enterprise Standard):
 * 1. Security (Prevents Mass Assignment / Over-Posting): A client must NEVER be able to inject
 *    read-only fields (like id, status, customerId, createdAt, assignedAgentId) during creation.
 *    By explicitly restricting the Request DTO to only user-input fields, the API prevents tampering.
 * 2. Strict Input Validation: Contains precise validation rules (NotBlank, Size) specifically
 *    tailored to ticket creation constraints.
 * 3. Independent Evolution: Request contracts can evolve without breaking response shapes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for submitting a new customer support ticket")
public class CreateTicketRequest {

    @NotBlank(message = "Ticket title is required")
    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    @Schema(description = "Brief summary of the issue", example = "Database connection timeout in production")
    private String title;

    @NotBlank(message = "Ticket description is required")
    @Size(min = 10, max = 5000, message = "Description must provide between 10 and 5000 characters")
    @Schema(description = "Detailed explanation of the problem or error logs", example = "The application cannot connect to PostgreSQL after the 2.4 update.")
    private String description;

    @NotNull(message = "Priority is required")
    @Schema(description = "Initial urgency level of the ticket", example = "HIGH")
    private TicketPriority priority;
}
