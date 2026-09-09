package com.support.mapper;

import com.support.dto.CreateTicketRequest;
import com.support.dto.TicketResponse;
import com.support.entity.Ticket;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * ==============================================================================================
 * MAPPER: TicketMapper (MapStruct)
 * ==============================================================================================
 * 
 * WHY WE USE SEPARATE REQUEST AND RESPONSE DTOS (Enterprise Standard):
 * 
 * 1. Over-Posting / Mass-Assignment Protection (OWASP Top 10):
 *    - Inbound requests (CreateTicketRequest) accept ONLY what the user is authorized to send
 *      (title, description, priority).
 *    - Server-managed fields (id, status, customerId, assignedAgentId, createdAt, history)
 *      are physically absent from the request model, making parameter injection impossible.
 * 
 * 2. Unambiguous API & Swagger Contracts:
 *    - Frontend clients view clean, exact documentation: CreateTicketRequest shows 3 required inputs,
 *      while TicketResponse documents the full entity with server-generated metadata.
 * 
 * 3. Validation Boundary Integrity:
 *    - Input validation constraints (@NotBlank, @Size, @NotNull) apply strictly to inbound requests
 *      without conflicting with server-returned response payloads.
 */
@Mapper(componentModel = "spring", uses = {TicketStatusHistoryMapper.class})
public interface TicketMapper {

    /**
     * Outbound Mapping: Entity -> Response DTO
     * Maps database entity and relational entities (Customer, Agent) to outbound JSON representation.
     */
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(source = "customer.username", target = "customerUsername")
    @Mapping(source = "assignedAgent.id", target = "assignedAgentId")
    @Mapping(source = "assignedAgent.username", target = "assignedAgentName")
    @Mapping(source = "statusHistory", target = "history")
    TicketResponse toResponse(Ticket ticket);

    /**
     * Outbound List Mapping: List<Entity> -> List<Response DTO>
     */
    List<TicketResponse> toResponseList(List<Ticket> tickets);

    /**
     * Inbound Mapping: Request DTO -> Entity
     * Safely constructs a new Ticket entity from user input fields only.
     * System fields (id, status, customer, agent, timestamps) are ignored during initial mapping
     * and set explicitly by the business service layer.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "assignedAgent", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "statusHistory", ignore = true)
    Ticket toEntity(CreateTicketRequest request);

    /**
     * Partial Entity Update Strategy:
     * Ignores null values when updating existing entities.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "assignedAgent", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "statusHistory", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(CreateTicketRequest request, @MappingTarget Ticket ticket);
}
