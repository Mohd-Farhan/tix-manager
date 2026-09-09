package com.support.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * ==============================================================================================
 * CONFIGURATION: JpaAuditConfig
 * ==============================================================================================
 * 
 * WHY SEPARATE AUDITING CONFIGURATION (Industry Best Practice):
 * 
 * Placing `@EnableJpaAuditing` on the main `@SpringBootApplication` class causes Spring Boot's
 * sliced tests (such as `@WebMvcTest`) to fail because `@WebMvcTest` only loads web layer components
 * and does not instantiate JPA entity managers or repositories.
 * 
 * By defining `@EnableJpaAuditing` in a dedicated `@Configuration` class, slice tests remain fast,
 * isolated, and free of JPA context dependencies.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditConfig {
}
