package com.support.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * ==============================================================================================
 * BASE ENTITY: Auditable
 * ==============================================================================================
 * 
 * WHY JPA AUDITING IS USED (Enterprise & Industry Standard):
 * 
 * 1. Regulatory Compliance & Non-Repudiation (SOC2 / ISO 27001 / GDPR):
 *    - Automatically captures WHO created or modified a database row and WHEN without requiring
 *      developers to write manual assignment boilerplate in business service methods.
 * 
 * 2. Declarative & Centralized Architecture:
 *    - Replaces scattered, error-prone `@PrePersist` and `@PreUpdate` lifecycle methods across
 *      individual entities with Spring Data's declarative `AuditingEntityListener`.
 *    - Seamlessly bridges Spring Security's `SecurityContext` via the `AuditorAware` SPI.
 * 
 * 3. Write-Protection for Audit Immutability:
 *    - `createdAt` and `createdBy` have `@Column(updatable = false)` to guarantee that database
 *      updates never alter the original author or insertion timestamp.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class Auditable {

    /**
     * Exact timestamp when the entity was first persisted.
     * Managed exclusively by Spring Data JPA; immutable after creation.
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when the entity was last updated.
     * Automatically refreshed by Spring Data JPA on every SQL UPDATE.
     */
    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * Username of the authenticated user who created the record.
     * Resolved dynamically from SecurityContextHolder via AuditorAware<String>.
     * Falls back to 'SYSTEM' for background tasks or initial DB seeding.
     */
    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    /**
     * Username of the user who performed the most recent modification.
     */
    @LastModifiedBy
    private String lastModifiedBy;
}
