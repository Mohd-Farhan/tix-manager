package com.support.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Summary result of bulk user upload operation")
public class BulkUploadResultDTO {

    @Schema(description = "Total number of candidate rows in the uploaded file", example = "25")
    private int totalRows;

    @Schema(description = "Number of users successfully created", example = "23")
    private int successCount;

    @Schema(description = "Number of rows that failed validation or insertion", example = "2")
    private int failureCount;

    @Builder.Default
    @Schema(description = "List of error descriptions for rows that failed processing")
    private List<String> errors = new ArrayList<>();
}
