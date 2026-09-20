package com.support.repository;

import com.support.entity.BulkUploadHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for immutable bulk upload audit history records.
 */
@Repository
public interface BulkUploadHistoryRepository extends JpaRepository<BulkUploadHistory, Long> {

    /**
     * Retrieve all bulk upload history records sorted with newest first.
     */
    List<BulkUploadHistory> findAllByOrderByCreatedAtDesc();

    /**
     * Retrieve paginated bulk upload history records sorted with newest first.
     */
    Page<BulkUploadHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
