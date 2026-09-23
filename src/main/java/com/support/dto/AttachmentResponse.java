package com.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ==============================================================================================
 * DTO: AttachmentResponse (Outbound Attachment Representation)
 * ==============================================================================================
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {

    private Long id;
    private Long ticketId;
    private Long messageId;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String fileSizeFormatted;
    private Long uploaderId;
    private String uploaderName;
    private LocalDateTime createdAt;
    private String previewUrl;
    private String downloadUrl;

    public static String formatFileSize(Long bytes) {
        if (bytes == null || bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        digitGroups = Math.min(digitGroups, units.length - 1);
        return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
