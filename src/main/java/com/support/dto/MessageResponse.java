package com.support.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ==============================================================================================
 * RESPONSE DTO: MessageResponse
 * ==============================================================================================
 * 
 * Outbound response representation of a message within a ticket conversation thread.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Outbound representation of a conversation thread message")
public class MessageResponse {

    @Schema(description = "Unique message identifier", example = "501")
    private Long id;

    @Schema(description = "Associated ticket identifier", example = "101")
    private Long ticketId;

    @Schema(description = "User ID of the message author", example = "5")
    private Long senderId;

    @Schema(description = "Username of the message author", example = "farhan_dev")
    private String senderUsername;

    @Schema(description = "Text content of the message", example = "We have deployed a hotfix and verified latency.")
    private String content;

    @Schema(description = "Timestamp when message was posted")
    private LocalDateTime createdAt;
}
