package com.support.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.support.entity.TicketPriority;
import com.support.entity.TicketStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketDTO {

    private Long id;

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    private String description;

    private TicketStatus status;

    @NotNull
    private TicketPriority priority;

    private Long customerId;

    private Long assignedAgentId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private boolean deleted;

    private List<TicketStatusHistoryDTO> history;
}
