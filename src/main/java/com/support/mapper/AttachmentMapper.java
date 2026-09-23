package com.support.mapper;

import com.support.dto.AttachmentResponse;
import com.support.entity.Attachment;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * ==============================================================================================
 * MAPPER: AttachmentMapper (MapStruct)
 * ==============================================================================================
 */
@Mapper(componentModel = "spring")
public interface AttachmentMapper {

    @Mapping(source = "ticket.id", target = "ticketId")
    @Mapping(source = "message.id", target = "messageId")
    @Mapping(source = "uploader.id", target = "uploaderId")
    @Mapping(source = "uploader.username", target = "uploaderName")
    AttachmentResponse toResponse(Attachment attachment);

    List<AttachmentResponse> toResponseList(List<Attachment> attachments);

    @AfterMapping
    default void populateComputedFields(@MappingTarget AttachmentResponse response, Attachment attachment) {
        if (attachment != null && attachment.getTicket() != null) {
            Long ticketId = attachment.getTicket().getId();
            Long attachmentId = attachment.getId();
            response.setPreviewUrl(String.format("/api/tickets/%d/attachments/%d/preview", ticketId, attachmentId));
            response.setDownloadUrl(String.format("/api/tickets/%d/attachments/%d/download", ticketId, attachmentId));
        }
        if (attachment != null && attachment.getFileSize() != null) {
            response.setFileSizeFormatted(AttachmentResponse.formatFileSize(attachment.getFileSize()));
        }
    }
}
