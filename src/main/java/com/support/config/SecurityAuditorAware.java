package com.support.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * ==============================================================================================
 * AUDITOR PROVIDER: SecurityAuditorAware
 * ==============================================================================================
 * 
 * WHY THIS IS USED (Enterprise Standard):
 * 
 * 1. Seamless Spring Security Integration:
 *    - Automatically pulls the current authenticated principal's username from `SecurityContextHolder`.
 *    - When an agent or admin modifies a ticket or user, their username is automatically recorded
 *      in `@CreatedBy` or `@LastModifiedBy`.
 * 
 * 2. Graceful Fallback for System / Seed Tasks:
 *    - If no authentication is present (e.g. during application startup, DB seeding in DataInitializer,
 *      or asynchronous batch jobs), defaults to "SYSTEM".
 */
@Component("auditorAware")
public class SecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of("SYSTEM");
        }

        return Optional.ofNullable(authentication.getName());
    }
}
