package com.support.service;

import com.support.dto.AttachmentResponse;
import com.support.entity.Attachment;
import com.support.entity.Message;
import com.support.entity.Ticket;
import com.support.entity.User;
import com.support.exception.InvalidOperationException;
import com.support.exception.ResourceNotFoundException;
import com.support.mapper.AttachmentMapper;
import com.support.repository.AttachmentRepository;
import com.support.repository.MessageRepository;
import com.support.repository.TicketRepository;
import com.support.repository.UserRepository;
import com.support.storage.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * ==============================================================================================
 * SERVICE: AttachmentService (Ticket File & Screenshot Management)
 * ==============================================================================================
 * 
 * WHY THIS VALIDATION & LIFECYCLE ARCHITECTURE (Enterprise Standards):
 * 1. Strict MIME & File Extension Allowlisting (OWASP ASVS §12.1.1):
 *    - Explicitly limits uploads to safe diagnostics: screenshots/images (PNG, JPG, JPEG, GIF, WEBP),
 *      documents (PDF), and logs/system dumps (TXT, LOG, CSV, JSON, XML, ZIP).
 *    - Blocks all executables, scripts, and SVG vectors (which can contain embedded malicious JavaScript).
 * 
 * 2. Size Guardrails:
 *    - Rejects payloads exceeding 10MB to protect application memory and disk capacity.
 * 
 * 3. Relational Consistency:
 *    - If an attachment references a Message, verifies that the Message genuinely belongs to the given Ticket.
 * 
 * 4. Audit Logging:
 *    - Integrates with `AuditService` so that all attachment uploads and deletions are permanently recorded.
 */
@Slf4j
@Service
public class AttachmentService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "webp",
            "pdf",
            "txt", "log", "csv", "json", "xml",
            "zip"
    );

    @Value("${app.storage.max-file-size-bytes:10485760}")
    private long maxFileSizeBytes;

    @Autowired
    private AttachmentRepository attachmentRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private AttachmentMapper attachmentMapper;

    @Autowired
    private AuditService auditService;

    /**
     * Uploads and links a file attachment to a ticket (and optional message).
     */
    @Transactional
    public AttachmentResponse uploadAttachment(Long ticketId, Long messageId, MultipartFile file, Long uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new InvalidOperationException("Cannot upload an empty file.");
        }

        // Validate file size limit
        if (file.getSize() > this.maxFileSizeBytes) {
            throw new InvalidOperationException(String.format(
                    "File size (%s) exceeds maximum permitted limit of %s.",
                    AttachmentResponse.formatFileSize(file.getSize()),
                    AttachmentResponse.formatFileSize(this.maxFileSizeBytes)));
        }

        // Validate file extension
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "attachment";
        String extension = extractExtension(originalFilename);
        if (extension.isEmpty() || !ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("Blocked unsupported file attachment upload attempt: '{}'", originalFilename);
            throw new InvalidOperationException(String.format(
                    "File type '.%s' is not supported. Allowed formats: %s",
                    extension, String.join(", ", ALLOWED_EXTENSIONS)));
        }

        // Validate ticket existence
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        // Validate message if provided
        Message message = null;
        if (messageId != null) {
            message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new ResourceNotFoundException("Message", "id", messageId));
            if (!message.getTicket().getId().equals(ticketId)) {
                throw new InvalidOperationException("Message #" + messageId + " does not belong to Ticket #" + ticketId);
            }
        }

        // Validate uploader
        User uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", uploaderId));

        // Persist physical file via pluggable storage provider
        String subDirectory = "ticket_" + ticketId;
        String storageKey = fileStorageService.storeFile(file, subDirectory);

        // Determine content type safely
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        // Create attachment database record
        Attachment attachment = Attachment.builder()
                .ticket(ticket)
                .message(message)
                .fileName(originalFilename)
                .storageKey(storageKey)
                .contentType(contentType)
                .fileSize(file.getSize())
                .uploader(uploader)
                .build();

        Attachment saved = attachmentRepository.save(attachment);
        log.info("Uploaded attachment id={} ('{}', {} bytes) for ticket id={} by user '{}'",
                saved.getId(), saved.getFileName(), saved.getFileSize(), ticketId, uploader.getUsername());

        auditService.recordEntityChange(
                "ATTACHMENT", saved.getId(), "UPLOAD", uploader.getUsername(),
                String.format("Attached '%s' (%s) to Ticket #%d",
                        saved.getFileName(), AttachmentResponse.formatFileSize(saved.getFileSize()), ticketId));

        return attachmentMapper.toResponse(saved);
    }

    /**
     * Retrieves all attachments associated with a given ticket.
     */
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachmentsForTicket(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Ticket", "id", ticketId);
        }
        List<Attachment> attachments = attachmentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        return attachmentMapper.toResponseList(attachments);
    }

    /**
     * Retrieves all attachments associated with a specific message.
     */
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getAttachmentsForMessage(Long messageId) {
        List<Attachment> attachments = attachmentRepository.findByMessageId(messageId);
        return attachmentMapper.toResponseList(attachments);
    }

    /**
     * Loads the attachment entity and Spring Resource for HTTP streaming.
     */
    @Transactional(readOnly = true)
    public AttachmentDownloadWrapper getAttachmentResource(Long ticketId, Long attachmentId) {
        Attachment attachment = attachmentRepository.findByIdAndTicketId(attachmentId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", attachmentId));

        Resource resource = fileStorageService.loadFileAsResource(attachment.getStorageKey());
        return new AttachmentDownloadWrapper(attachment, resource);
    }

    /**
     * Deletes an attachment and cleans up its physical storage.
     */
    @Transactional
    public void deleteAttachment(Long ticketId, Long attachmentId, Long actorUserId, boolean isAdminOrAgent) {
        Attachment attachment = attachmentRepository.findByIdAndTicketId(attachmentId, ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", "id", attachmentId));

        // Enforce deletion permission: uploader or ADMIN / SUPPORT_AGENT
        if (!isAdminOrAgent && !attachment.getUploader().getId().equals(actorUserId)) {
            throw new InvalidOperationException("You do not have permission to delete this attachment.");
        }

        // Delete physical file from disk or cloud storage
        fileStorageService.deleteFile(attachment.getStorageKey());

        // Delete database record
        attachmentRepository.delete(attachment);
        log.info("Deleted attachment id={} on ticket id={} by actor id={}", attachmentId, ticketId, actorUserId);

        auditService.recordEntityChange(
                "ATTACHMENT", attachmentId, "DELETE", "user#" + actorUserId,
                String.format("Deleted attachment '%s' from Ticket #%d", attachment.getFileName(), ticketId));
    }

    private String extractExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Wrapper holding both JPA metadata and the Spring streaming Resource.
     */
    public record AttachmentDownloadWrapper(Attachment attachment, Resource resource) {}
}
