package com.support.repository;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.support.entity.Message;

/**
 * ==============================================================================================
 * REPOSITORY: MessageRepository
 * ==============================================================================================
 * 
 * WHY QUERY OPTIMIZATION & @EntityGraph ARE USED:
 * - `@EntityGraph(attributePaths = {"sender"})` eagerly fetches the message sender in the same
 *   SQL query as the message content, avoiding lazy initialization errors and N+1 roundtrips.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    @EntityGraph(attributePaths = {"sender"})
    List<Message> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}