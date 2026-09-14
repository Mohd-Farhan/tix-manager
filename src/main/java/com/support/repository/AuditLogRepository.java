package com.support.repository;

import com.support.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityNameAndEntityIdOrderByCreatedAtDesc(String entityName, Long entityId);

    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT a.id FROM AuditLog a WHERE a.createdAt < :cutoff ORDER BY a.createdAt ASC")
    List<Long> findOldAuditLogIds(@Param("cutoff") LocalDateTime cutoff, Pageable pageable);

    @Modifying
    @Query("DELETE FROM AuditLog a WHERE a.id IN :ids")
    int deleteBatchByIds(@Param("ids") List<Long> ids);
}
