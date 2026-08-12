package com.support.mapper;

import com.support.dto.MessageDTO;
import com.support.entity.Message;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(source = "ticket.id",      target = "ticketId")
    @Mapping(source = "sender.id",      target = "senderId")
    @Mapping(source = "sender.username", target = "senderUsername")
    MessageDTO toDTO(Message message);

    List<MessageDTO> toDTOList(List<Message> messages);

    @Mapping(target = "ticket",    ignore = true)
    @Mapping(target = "sender",    ignore = true)
    @Mapping(target = "id",        ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Message toEntity(MessageDTO dto);
}
