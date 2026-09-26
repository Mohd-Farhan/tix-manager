package com.support.dto;

import com.support.entity.TicketPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ==============================================================================================
 * DTO: PriorityUpdateRequest (Manual Ticket Priority Escalation / Adjustment)
 * ==============================================================================================
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for adjusting ticket priority level")
public class PriorityUpdateRequest {

    @NotNull(message = "Priority is required")
    @Schema(description = "New priority level to assign", example = "HIGH")
    private TicketPriority priority;
}
