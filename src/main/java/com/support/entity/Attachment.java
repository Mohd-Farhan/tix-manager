package com.support.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * ==============================================================================================
 * ENTITY: Attachment (Ticket & Message File / Screenshot Attachment)
 * ==============================================================================================
 * 
 * WHY THIS DATABASE & STORAGE MODEL (Enterprise Standards):
 * 1. Storage Key Decoupling (Path Traversal & Security Isolation):
 *    - The `fileName` stores the user's original sanitized filename for UI presentation.
 *    - The `storageKey` stores a cryptographically isolated UUID identifier on disk or S3 object store.
 *    - Client uploads NEVER control the physical path on the filesystem, eliminating path traversal attacks.
 * 
 * 2. Polymorphic Association (Ticket vs Message):
 *    - An attachment is ALWAYS tied to a root `Ticket` (`ticket_id` NOT NULL) for authorization boundary enforcement.
 *    - An attachment MAY be tied to a specific `Message` (`message_id` nullable), allowing inline rendering
 *      within conversation thread bubbles.
 * 
 * 3. Selective Indexing:
 *    - `idx_attachment_ticket_id`: Fast retrieval of all files belonging to a ticket for the sidebar gallery.
 *    - `idx_attachment_message_id`: Fast retrieval of files attached to a specific message bubble.
 */
@Entity
@Table(name = "attachments", indexes = {
        @Index(name = "idx_attachment_ticket_id", columnList = "ticket_id"),
        @Index(name = "idx_attachment_message_id", columnList = "message_id")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    @NotNull
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private Message message;

    @Column(nullable = false)
    @NotBlank
    private String fileName;

    @Column(nullable = false, unique = true)
    @NotBlank
    private String storageKey;

    @Column(nullable = false)
    @NotBlank
    private String contentType;

    @Column(nullable = false)
    @NotNull
    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    @NotNull
    private User uploader;
}
