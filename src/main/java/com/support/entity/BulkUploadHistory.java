package com.support.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ==============================================================================================
 * ENTITY: BulkUploadHistory (Immutable Audit Record for Bulk User Imports)
 * ==============================================================================================
 * 
 * DESIGN PRINCIPLES:
 * 1. Strict Immutability & Non-Repudiation:
 *    - Append-only historical table. Records can never be modified once persisted.
 *    - All entity columns enforce `updatable = false` at the JPA/Hibernate metadata layer.
 *    - No public setters exposed to prevent inadvertent in-memory state changes.
 * 
 * 2. JPA Auditing Integration:
 *    - Captures exact persist timestamp (`createdAt`) and caller principal (`createdBy`).
 * 
 * 3. Sanitized Storage:
 *    - Stores batch ingestion statistics and error reasons.
 *    - NEVER stores raw files or sensitive credentials (passwords).
 */
@Entity
@Table(name = "bulk_upload_history", indexes = {
    @Index(name = "idx_bulk_upload_created_at", columnList = "created_at DESC"),
    @Index(name = "idx_bulk_upload_uploaded_by", columnList = "uploaded_by")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class BulkUploadHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, updatable = false, length = 255)
    private String fileName;

    @Column(name = "uploaded_by", nullable = false, updatable = false, length = 100)
    private String uploadedBy;

    @Column(name = "total_rows", nullable = false, updatable = false)
    private int totalRows;

    @Column(name = "success_count", nullable = false, updatable = false)
    private int successCount;

    @Column(name = "failure_count", nullable = false, updatable = false)
    private int failureCount;

    @Column(name = "status", nullable = false, updatable = false, length = 50)
    private String status; // 'SUCCESS', 'PARTIAL_SUCCESS', 'FAILED'

    @Column(name = "error_details", updatable = false, columnDefinition = "TEXT")
    private String errorDetails;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;
}
