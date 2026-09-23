package com.support.service;

import com.support.dto.AttachmentResponse;
import com.support.entity.*;
import com.support.exception.InvalidOperationException;
import com.support.exception.ResourceNotFoundException;
import com.support.mapper.AttachmentMapper;
import com.support.repository.AttachmentRepository;
import com.support.repository.MessageRepository;
import com.support.repository.TicketRepository;
import com.support.repository.UserRepository;
import com.support.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttachmentServiceTest {

    @Mock
    private AttachmentRepository attachmentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AttachmentMapper attachmentMapper;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AttachmentService attachmentService;

    private Ticket ticket;
    private User customer;
    private User agent;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(attachmentService, "maxFileSizeBytes", 10485760L);

        customer = new User();
        customer.setId(10L);
        customer.setUsername("customer1");
        customer.setRole(UserRole.CUSTOMER);

        agent = new User();
        agent.setId(20L);
        agent.setUsername("agent1");
        agent.setRole(UserRole.SUPPORT_AGENT);

        ticket = new Ticket();
        ticket.setId(100L);
        ticket.setTitle("System Crash");
        ticket.setCustomer(customer);
    }

    @Test
    @DisplayName("uploadAttachment — successfully validates, stores, and saves attachment")
    void testUploadAttachment_Success() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "error_log.txt", "text/plain", "Traceback line 42".getBytes());

        when(ticketRepository.findById(100L)).thenReturn(Optional.of(ticket));
        when(userRepository.findById(10L)).thenReturn(Optional.of(customer));
        when(fileStorageService.storeFile(eq(file), eq("ticket_100"))).thenReturn("ticket_100/uuid_error_log.txt");

        Attachment savedAttachment = Attachment.builder()
                .id(1L)
                .ticket(ticket)
                .fileName("error_log.txt")
                .storageKey("ticket_100/uuid_error_log.txt")
                .contentType("text/plain")
                .fileSize((long) "Traceback line 42".length())
                .uploader(customer)
                .build();
        when(attachmentRepository.save(any(Attachment.class))).thenReturn(savedAttachment);

        AttachmentResponse expectedResponse = AttachmentResponse.builder()
                .id(1L)
                .ticketId(100L)
                .fileName("error_log.txt")
                .fileSizeFormatted("21.0 B")
                .build();
        when(attachmentMapper.toResponse(savedAttachment)).thenReturn(expectedResponse);

        AttachmentResponse response = attachmentService.uploadAttachment(100L, null, file, 10L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getFileName()).isEqualTo("error_log.txt");

        verify(fileStorageService, times(1)).storeFile(file, "ticket_100");
        verify(attachmentRepository, times(1)).save(any(Attachment.class));
        verify(auditService, times(1)).recordEntityChange(eq("ATTACHMENT"), eq(1L), eq("UPLOAD"), eq("customer1"), anyString());
    }

    @Test
    @DisplayName("uploadAttachment — rejects disallowed file extensions (e.g. .exe, .svg)")
    void testUploadAttachment_UnsupportedExtension() {
        MockMultipartFile exeFile = new MockMultipartFile(
                "file", "malware.exe", "application/x-msdownload", "binary-code".getBytes());

        assertThatThrownBy(() -> attachmentService.uploadAttachment(100L, null, exeFile, 10L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("File type '.exe' is not supported");

        MockMultipartFile svgFile = new MockMultipartFile(
                "file", "vector.svg", "image/svg+xml", "<svg></svg>".getBytes());

        assertThatThrownBy(() -> attachmentService.uploadAttachment(100L, null, svgFile, 10L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("File type '.svg' is not supported");

        verifyNoInteractions(fileStorageService, attachmentRepository);
    }

    @Test
    @DisplayName("uploadAttachment — rejects file exceeding max file size limit")
    void testUploadAttachment_ExceedsSizeLimit() {
        byte[] oversizedBytes = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "dump.log", "text/plain", oversizedBytes);

        assertThatThrownBy(() -> attachmentService.uploadAttachment(100L, null, largeFile, 10L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("exceeds maximum permitted limit");

        verifyNoInteractions(fileStorageService, attachmentRepository);
    }

    @Test
    @DisplayName("uploadAttachment — throws ResourceNotFoundException when ticket does not exist")
    void testUploadAttachment_TicketNotFound() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "screenshot.png", "image/png", "image-content".getBytes());

        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attachmentService.uploadAttachment(999L, null, file, 10L))
                .isInstanceOf(ResourceNotFoundException.class);

        verifyNoInteractions(fileStorageService);
    }

    @Test
    @DisplayName("deleteAttachment — successfully deletes when actor is uploader")
    void testDeleteAttachment_SuccessByUploader() {
        Attachment attachment = Attachment.builder()
                .id(5L)
                .ticket(ticket)
                .fileName("screenshot.png")
                .storageKey("ticket_100/uuid_screenshot.png")
                .uploader(customer)
                .build();

        when(attachmentRepository.findByIdAndTicketId(5L, 100L)).thenReturn(Optional.of(attachment));

        attachmentService.deleteAttachment(100L, 5L, 10L, false);

        verify(fileStorageService, times(1)).deleteFile("ticket_100/uuid_screenshot.png");
        verify(attachmentRepository, times(1)).delete(attachment);
        verify(auditService, times(1)).recordEntityChange(eq("ATTACHMENT"), eq(5L), eq("DELETE"), anyString(), anyString());
    }

    @Test
    @DisplayName("deleteAttachment — throws InvalidOperationException when non-owner non-admin attempts delete")
    void testDeleteAttachment_ForbiddenByNonOwner() {
        Attachment attachment = Attachment.builder()
                .id(5L)
                .ticket(ticket)
                .fileName("screenshot.png")
                .storageKey("ticket_100/uuid_screenshot.png")
                .uploader(customer)
                .build();

        when(attachmentRepository.findByIdAndTicketId(5L, 100L)).thenReturn(Optional.of(attachment));

        assertThatThrownBy(() -> attachmentService.deleteAttachment(100L, 5L, 99L, false))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("do not have permission to delete this attachment");

        verify(fileStorageService, never()).deleteFile(anyString());
        verify(attachmentRepository, never()).delete(any(Attachment.class));
    }
}
