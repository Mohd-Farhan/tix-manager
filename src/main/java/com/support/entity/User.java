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
 * - Unique columns `username` and `email` have `@Column(unique = true)`, which
 * automatically creates
 * unique B-Tree indexes at the database level.
 * - Redundant table-level index annotations were removed to avoid duplicate
 * index maintenance overhead.
 */
@Entity
@Table(name = "users")
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
}