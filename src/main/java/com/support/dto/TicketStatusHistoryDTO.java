package com.support.dto;

import java.time.LocalDateTime;

import com.support.entity.TicketStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketStatusHistoryDTO {

    private Long id;

    private Long ticketId;

    private TicketStatus previousStatus;

    private TicketStatus newStatus;

    private Long changedById;

    private String changedByUsername;

    private LocalDateTime changedAt;
}
