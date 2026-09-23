package com.support.controller;

import com.support.config.GlobalExceptionHandler;
import com.support.dto.AttachmentResponse;
import com.support.entity.Attachment;
import com.support.service.AttachmentService;
import com.support.service.AttachmentService.AttachmentDownloadWrapper;
import com.support.util.SecurityUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AttachmentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AttachmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AttachmentService attachmentService;

    @MockBean
    private SecurityUtils securityUtils;

    @MockBean
    private com.support.security.JwtService jwtService;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @MockBean
    private com.support.security.TicketSecurity ticketSecurity;

    @Test
    @DisplayName("POST /api/tickets/{ticketId}/attachments — upload file successfully")
    void testUploadAttachment_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "error.log", "text/plain", "Error occurred at line 42".getBytes());

        AttachmentResponse response = AttachmentResponse.builder()
                .id(1L)
                .ticketId(100L)
                .fileName("error.log")
                .contentType("text/plain")
                .fileSize(29L)
                .fileSizeFormatted("29 B")
                .uploaderName("Alice")
                .previewUrl("/api/tickets/100/attachments/1/preview")
                .downloadUrl("/api/tickets/100/attachments/1/download")
                .createdAt(LocalDateTime.now())
                .build();

        when(securityUtils.resolveUserId(any())).thenReturn(5L);
        when(attachmentService.uploadAttachment(eq(100L), any(), any(), eq(5L))).thenReturn(response);

        mockMvc.perform(multipart("/api/tickets/100/attachments")
                        .file(file)
                        .param("messageId", "200"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fileName").value("error.log"))
                .andExpect(jsonPath("$.downloadUrl").value("/api/tickets/100/attachments/1/download"));
    }

    @Test
    @DisplayName("GET /api/tickets/{ticketId}/attachments — list attachments")
    void testGetTicketAttachments_Success() throws Exception {
        AttachmentResponse response = AttachmentResponse.builder()
                .id(1L)
                .ticketId(100L)
                .fileName("screenshot.png")
                .contentType("image/png")
                .fileSize(1024L)
                .fileSizeFormatted("1.0 KB")
                .uploaderName("Bob")
                .previewUrl("/api/tickets/100/attachments/1/preview")
                .downloadUrl("/api/tickets/100/attachments/1/download")
                .createdAt(LocalDateTime.now())
                .build();

        when(attachmentService.getAttachmentsForTicket(100L)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/tickets/100/attachments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fileName").value("screenshot.png"));
    }

    @Test
    @DisplayName("GET /api/tickets/{ticketId}/attachments/{id}/preview — inline stream")
    void testPreviewAttachment_Success() throws Exception {
        Attachment attachment = Attachment.builder()
                .id(1L)
                .fileName("screenshot.png")
                .contentType("image/png")
                .build();
        Resource resource = new ByteArrayResource("image-bytes".getBytes());
        AttachmentDownloadWrapper wrapper = new AttachmentDownloadWrapper(attachment, resource);

        when(attachmentService.getAttachmentResource(100L, 1L)).thenReturn(wrapper);

        mockMvc.perform(get("/api/tickets/100/attachments/1/preview"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"screenshot.png\""))
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes("image-bytes".getBytes()));
    }

    @Test
    @DisplayName("GET /api/tickets/{ticketId}/attachments/{id}/download — download attachment stream")
    void testDownloadAttachment_Success() throws Exception {
        Attachment attachment = Attachment.builder()
                .id(2L)
                .fileName("dump.log")
                .contentType("text/plain")
                .build();
        Resource resource = new ByteArrayResource("log content".getBytes());
        AttachmentDownloadWrapper wrapper = new AttachmentDownloadWrapper(attachment, resource);

        when(attachmentService.getAttachmentResource(100L, 2L)).thenReturn(wrapper);

        mockMvc.perform(get("/api/tickets/100/attachments/2/download"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"dump.log\""))
                .andExpect(content().contentType(MediaType.TEXT_PLAIN))
                .andExpect(content().bytes("log content".getBytes()));
    }

    @Test
    @DisplayName("DELETE /api/tickets/{ticketId}/attachments/{id} — delete attachment returns 204")
    void testDeleteAttachment_Success() throws Exception {
        when(securityUtils.resolveUserId(any())).thenReturn(5L);
        doNothing().when(attachmentService).deleteAttachment(eq(100L), eq(1L), eq(5L), org.mockito.ArgumentMatchers.anyBoolean());

        mockMvc.perform(delete("/api/tickets/100/attachments/1"))
                .andExpect(status().isNoContent());
    }
}
