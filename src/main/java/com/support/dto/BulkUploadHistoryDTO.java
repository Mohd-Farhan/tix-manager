package com.support.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Historical record of a bulk user upload batch")
public class BulkUploadHistoryDTO {

    @Schema(description = "Unique audit record ID", example = "1")
    private Long id;

    @Schema(description = "Uploaded CSV filename", example = "q3_support_agents.csv")
    private String fileName;

    @Schema(description = "Administrator who triggered the batch upload", example = "sysadmin")
    private String uploadedBy;

    @Schema(description = "Total data rows parsed from the CSV", example = "45")
    private int totalRows;

    @Schema(description = "Count of accounts successfully created", example = "42")
    private int successCount;

    @Schema(description = "Count of rows rejected due to validation or hierarchy errors", example = "3")
    private int failureCount;

    @Schema(description = "Overall batch status (SUCCESS, PARTIAL_SUCCESS, FAILED)", example = "PARTIAL_SUCCESS")
    private String status;

    @Builder.Default
    @Schema(description = "Itemized row-by-row error reasons")
    private List<String> errors = new ArrayList<>();

    @Schema(description = "Timestamp when the upload occurred")
    private LocalDateTime createdAt;
}
