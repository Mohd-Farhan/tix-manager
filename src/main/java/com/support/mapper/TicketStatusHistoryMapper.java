package com.support.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.support.dto.TicketStatusHistoryDTO;
import com.support.entity.TicketStatusHistory;

@Mapper(componentModel = "spring")
public interface TicketStatusHistoryMapper {

    @Mapping(source = "ticket.id", target = "ticketId")
    @Mapping(source = "changedBy.id", target = "changedById")
    @Mapping(source = "changedBy.username", target = "changedByUsername")
    TicketStatusHistoryDTO toDTO(TicketStatusHistory ticketStatusHistory);

    List<TicketStatusHistoryDTO> toDTOList(List<TicketStatusHistory> historyList);

}
