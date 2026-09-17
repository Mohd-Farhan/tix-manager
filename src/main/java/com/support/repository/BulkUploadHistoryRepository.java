package com.support.repository;

import com.support.entity.BulkUploadHistory;
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
}
