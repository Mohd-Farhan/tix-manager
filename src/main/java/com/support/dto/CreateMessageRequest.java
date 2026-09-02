package com.support.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ==============================================================================================
 * REQUEST DTO: CreateMessageRequest
 * ==============================================================================================
 * 
 * WHY SEPARATE REQUEST DTO FROM RESPONSE DTO (Enterprise Standard):
 * 1. Payload Precision: Clients only send the message body string; sender ID is securely resolved
 *    from the authenticated session token, and timestamps are generated server-side.
 * 2. Input Sanitization & Constraints: Enforces non-blank content and length boundaries.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload for appending a message to a ticket conversation thread")
public class CreateMessageRequest {

    @NotBlank(message = "Message content cannot be empty")
    @Size(max = 5000, message = "Message cannot exceed 5000 characters")
    @Schema(description = "Text content of the message", example = "We have deployed a hotfix and verified latency is back to normal.")
    private String content;
}
