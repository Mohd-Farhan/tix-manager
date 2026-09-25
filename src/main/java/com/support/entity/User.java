package com.support.entity;

import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * ==============================================================================================
 * ENTITY: User (System Identity & RBAC Account)
 * ==============================================================================================
 * 
 * WHY THIS DATABASE DESIGN:
 * - Unique columns `username` and `email` have `@Column(unique = true)`, which automatically creates
 *   unique B-Tree indexes at the database level.
 * - Performance indexes added:
 *   - `idx_user_role`: Speeds up agent roster lookups and role-based filtering (`findByRole`).
 *   - `idx_user_deleted`: Optimizes `@SQLRestriction("deleted = false")` on every user query.
 *   - `idx_user_created_at`: Eliminates table sorting overhead for paginated user views.
 */
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_role", columnList = "role"),
        @Index(name = "idx_user_deleted", columnList = "deleted"),
        @Index(name = "idx_user_created_at", columnList = "created_at DESC")
})
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("deleted = false")
public class User extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @Column(nullable = false, unique = true)
    @NotBlank
    @Email
    private String email;

    @Column(nullable = false)
    @NotBlank
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.CUSTOMER;

    @Column(nullable = false)
    private boolean deleted = false;

    /**
     * ==============================================================================================
     * NIST SP 800-63B §5.1.1.2 & SOC2 CC6.1 COMPLIANCE: Initial Credential Lifecycle
     * ==============================================================================================
     * 
     * WHY THIS IS USED:
     * 1. Protection Against Known Default Passwords:
     *    - When administrators provision accounts or import users via bulk CSV, accounts are
     *      initialized with temporary/shared credentials (e.g. `username@123`).
     *    - Flagging `mustChangePassword = true` ensures these predictable credentials cannot be
     *      exploited for unauthorized access beyond the initial login session.
     * 
     * 2. Mandatory First-Login Remediation:
     *    - The application layer intercepts authentication and forces the user to provide an
     *      explicit, compliant personal password before granting access to operational dashboards.
     *    - Successfully updating the password resets this flag to `false`.
     */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = false;

    /**
     * Timestamp of latest password modification.
     * Any JWT token issued prior to this timestamp is instantly rejected in real-time.
     */
    @Column(name = "password_changed_at")
    private java.time.LocalDateTime passwordChangedAt;

    /**
     * Timestamp of latest user logout or global session termination.
     * Any JWT token issued prior to this timestamp is instantly rejected in real-time.
     */
    @Column(name = "last_logout_at")
    private java.time.LocalDateTime lastLogoutAt;
}