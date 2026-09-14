package com.support.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ==============================================================================================
 * CONFIGURATION: SchedulingConfig
 * ==============================================================================================
 * 
 * WHY SEPARATE SCHEDULING CONFIGURATION:
 * 
 * Placing `@EnableScheduling` on the main `@SpringBootApplication` class causes sliced tests
 * (e.g. `@WebMvcTest`) to attempt to initialize scheduled tasks or background threads.
 * 
 * By placing `@EnableScheduling` in this dedicated `@Configuration` class, slice tests remain
 * isolated and fast.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
