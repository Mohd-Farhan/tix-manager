package com.support.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageDTO {

    private Long id;

    private Long ticketId;

    private Long senderId;

    private String senderUsername;

    @NotBlank
    private String content;

    private LocalDateTime createdAt;
}
