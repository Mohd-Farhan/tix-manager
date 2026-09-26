package com.support.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ==============================================================================================
 * DTO: SlaMetricsDTO (Aggregated Operational SLA Compliance Metrics)
 * ==============================================================================================
 * 
 * Provides high-level operational SLA health statistics for agent and admin dashboards.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "High-level SLA compliance and breach operational metrics")
public class SlaMetricsDTO {

    @Schema(description = "Total number of currently active tickets (OPEN or IN_PROGRESS)", example = "42")
    private long totalActive;

    @Schema(description = "Active tickets safely within SLA deadline (> 2h remaining)", example = "35")
    private long withinSla;

    @Schema(description = "Active tickets approaching SLA breach (<= 2h remaining)", example = "4")
    private long nearBreach;

    @Schema(description = "Active tickets that have exceeded SLA deadline", example = "3")
    private long breached;

    @Schema(description = "Total tickets in terminal RESOLVED status", example = "150")
    private long totalResolved;

    @Schema(description = "Resolved tickets that met SLA compliance deadline", example = "142")
    private long resolvedWithinSla;

    @Schema(description = "Historical SLA compliance percentage rate (0.0 to 100.0)", example = "94.67")
    private double complianceRatePercent;
}
