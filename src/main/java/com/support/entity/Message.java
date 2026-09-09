package com.support.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import lombok.*;

/**
 * ==============================================================================================
 * ENTITY: Message (Ticket Thread Conversation Entry)
 * ==============================================================================================
 * 
 * WHY THIS DATABASE DESIGN:
 * - `idx_message_ticket_id`: Fast retrieval of conversation threads by ticket
 * ID.
 * - Single index on ticket_id is sufficient since messages are always loaded
 * per ticket.
 */
@Entity
@Table(name = "messages", indexes = {
        @Index(name = "idx_message_ticket_id", columnList = "ticket_id")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
public class Message extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank
    private String content;
}