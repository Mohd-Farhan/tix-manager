package com.support.repository;

import com.support.entity.Attachment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ==============================================================================================
 * REPOSITORY: AttachmentRepository
 * ==============================================================================================
 * 
 * Target entity queries for ticket attachments with eager uploader joins to prevent N+1 query overhead.
 */
@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    @EntityGraph(attributePaths = {"uploader"})
    List<Attachment> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    @EntityGraph(attributePaths = {"uploader"})
    List<Attachment> findByMessageId(Long messageId);

    @EntityGraph(attributePaths = {"uploader"})
    Optional<Attachment> findByIdAndTicketId(Long id, Long ticketId);

    long countByTicketId(Long ticketId);
}
