package com.support.mapper;

import com.support.dto.CreateMessageRequest;
import com.support.dto.MessageResponse;
import com.support.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * ==============================================================================================
 * MAPPER: MessageMapper (MapStruct)
 * ==============================================================================================
 * 
 * WHY WE USE SEPARATE REQUEST AND RESPONSE DTOS (Enterprise Standard):
 * 
 * 1. Sender Identity Protection:
 *    - Inbound (CreateMessageRequest) accepts only the message `content`.
 *    - The `senderId` is resolved from authenticated JWT security context, preventing users
 *      from spoofing messages on behalf of other accounts.
 * 
 * 2. Outbound Context:
 *    - Outbound (MessageResponse) includes relational author metadata (`senderUsername`, `createdAt`, `ticketId`).
 */
@Mapper(componentModel = "spring")
public interface MessageMapper {

    /**
     * Outbound Mapping: Entity -> Response DTO
     */
    @Mapping(source = "ticket.id", target = "ticketId")
    @Mapping(source = "sender.id", target = "senderId")
    @Mapping(source = "sender.username", target = "senderUsername")
    MessageResponse toResponse(Message message);

    /**
     * Outbound List Mapping: List<Entity> -> List<Response DTO>
     */
    List<MessageResponse> toResponseList(List<Message> messages);

    /**
     * Inbound Mapping: Request DTO -> Entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ticket", ignore = true)
    @Mapping(target = "sender", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Message toEntity(CreateMessageRequest request);
}
