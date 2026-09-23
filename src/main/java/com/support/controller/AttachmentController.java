package com.support.controller;

import com.support.dto.AttachmentResponse;
import com.support.service.AttachmentService;
import com.support.service.AttachmentService.AttachmentDownloadWrapper;
import com.support.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * ==============================================================================================
 * REST CONTROLLER: AttachmentController
 * ==============================================================================================
 * 
 * WHY FINE-GRAINED OBJECT LEVEL ACCESS CONTROL:
 * - Every endpoint in this controller evaluates `@PreAuthorize("@ticketSecurity.canAccessTicket(#ticketId, authentication)")`.
 * - Prevents Insecure Direct Object Reference (IDOR) attacks where a customer attempts to query or
 *   download sensitive diagnostic logs from another customer's ticket.
 */
@Slf4j
@RestController
@RequestMapping("/api/tickets/{ticketId}/attachments")
@Tag(name = "Ticket Attachments", description = "Endpoints for uploading, previewing, and downloading ticket screenshots and files")
public class AttachmentController {

    @Autowired
    private AttachmentService attachmentService;

    @Autowired
    private SecurityUtils securityUtils;

    @Operation(summary = "Upload attachment to ticket", description = "Uploads an image, screenshot, or diagnostic log to the given ticket or message.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Attachment uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "File is empty, exceeds limit, or has unsupported extension"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Cannot access this ticket"),
            @ApiResponse(responseCode = "404", description = "Ticket or Message not found")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@ticketSecurity.canAccessTicket(#ticketId, authentication)")
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @PathVariable Long ticketId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "messageId", required = false) Long messageId,
            Authentication authentication) {
        Long uploaderId = securityUtils.resolveUserId(authentication);
        log.info("REST: Uploading attachment '{}' for ticket id={} (messageId={}) by userId={}",
                file.getOriginalFilename(), ticketId, messageId, uploaderId);

        AttachmentResponse response = attachmentService.uploadAttachment(ticketId, messageId, file, uploaderId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List all ticket attachments", description = "Retrieves metadata for all files attached to this ticket and its thread messages.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attachments listed successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Cannot access this ticket"),
            @ApiResponse(responseCode = "404", description = "Ticket not found")
    })
    @GetMapping
    @PreAuthorize("@ticketSecurity.canAccessTicket(#ticketId, authentication)")
    public ResponseEntity<List<AttachmentResponse>> getTicketAttachments(@PathVariable Long ticketId) {
        List<AttachmentResponse> list = attachmentService.getAttachmentsForTicket(ticketId);
        return ResponseEntity.ok(list);
    }

    @Operation(summary = "Preview attachment inline", description = "Streams file byte contents with inline Content-Disposition for browser display (images, PDF).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File stream returned"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Cannot access this ticket"),
            @ApiResponse(responseCode = "404", description = "Attachment not found")
    })
    @GetMapping("/{attachmentId}/preview")
    @PreAuthorize("@ticketSecurity.canAccessTicket(#ticketId, authentication)")
    public ResponseEntity<Resource> previewAttachment(
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId) {
        AttachmentDownloadWrapper wrapper = attachmentService.getAttachmentResource(ticketId, attachmentId);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(wrapper.attachment().getContentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + wrapper.attachment().getFileName() + "\"")
                .body(wrapper.resource());
    }

    @Operation(summary = "Download attachment", description = "Streams file byte contents with attachment Content-Disposition to trigger file download dialog.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File download stream returned"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Cannot access this ticket"),
            @ApiResponse(responseCode = "404", description = "Attachment not found")
    })
    @GetMapping("/{attachmentId}/download")
    @PreAuthorize("@ticketSecurity.canAccessTicket(#ticketId, authentication)")
    public ResponseEntity<Resource> downloadAttachment(
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId) {
        AttachmentDownloadWrapper wrapper = attachmentService.getAttachmentResource(ticketId, attachmentId);

        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(wrapper.attachment().getContentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + wrapper.attachment().getFileName() + "\"")
                .body(wrapper.resource());
    }

    @Operation(summary = "Delete attachment", description = "Permanently deletes an attachment. Requires uploader ownership or ADMIN / SUPPORT_AGENT role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Attachment deleted successfully"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Cannot access this ticket or delete this file"),
            @ApiResponse(responseCode = "404", description = "Attachment not found")
    })
    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("@ticketSecurity.canAccessTicket(#ticketId, authentication)")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long ticketId,
            @PathVariable Long attachmentId,
            Authentication authentication) {
        Long actorId = securityUtils.resolveUserId(authentication);
        boolean isAdminOrAgent = authentication != null && authentication.getAuthorities() != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPPORT_AGENT"));

        attachmentService.deleteAttachment(ticketId, attachmentId, actorId, isAdminOrAgent);
        return ResponseEntity.noContent().build();
    }
}
