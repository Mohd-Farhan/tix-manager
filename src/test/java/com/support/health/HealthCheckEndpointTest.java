package com.support.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ==============================================================================================
 * AUTOMATED TESTING SUITE: HealthCheckEndpointTest
 * ==============================================================================================
 * 
 * Verifies Spring Boot Actuator health and container orchestration probes:
 * 1. /actuator/health — Overall application health with custom TixManager indicators.
 * 2. /actuator/health/liveness — Kubernetes liveness probe.
 * 3. /actuator/health/readiness — Kubernetes readiness probe (database & thread pool verification).
 * 4. /actuator/metrics — APM and container metrics discovery.
 */
@SpringBootTest
@AutoConfigureMockMvc
class HealthCheckEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /actuator/health — Overall health is UP and contains TixManager custom indicator")
    void testActuatorHealth() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.tixManager.status").value("UP"))
                .andExpect(jsonPath("$.components.tixManager.details.database").value("CONNECTED"));
    }

    @Test
    @DisplayName("GET /actuator/health/liveness — Kubernetes liveness probe returns UP")
    void testKubernetesLivenessProbe() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /actuator/health/readiness — Kubernetes readiness probe returns UP")
    void testKubernetesReadinessProbe() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /actuator/metrics — Actuator metrics endpoint is exposed and available")
    void testActuatorMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names").isArray());
    }
}
