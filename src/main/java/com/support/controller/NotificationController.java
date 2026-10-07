package com.support.controller;

import com.support.dto.NotificationResponse;
import com.support.service.NotificationService;
import com.support.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ==============================================================================================
 * CONTROLLER: NotificationController (In-App Alerts API)
 * ==============================================================================================
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "In-App Notifications & Alerts (Observer Pattern consumer)")
public class NotificationController {

    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @Autowired
    public NotificationController(NotificationService notificationService, SecurityUtils securityUtils) {
        this.notificationService = notificationService;
        this.securityUtils = securityUtils;
    }

    @Operation(summary = "Get user notifications", description = "Retrieves in-app notifications for the authenticated user.")
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(Authentication authentication) {
        Long userId = securityUtils.resolveUserId(authentication);
        List<NotificationResponse> notifications = notificationService.getNotificationsForUser(userId);
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "Get unread notifications count", description = "Returns the number of unread alerts for the authenticated user.")
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        Long userId = securityUtils.resolveUserId(authentication);
        long count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @Operation(summary = "Mark notification as read", description = "Flags a specific notification as read.")
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = securityUtils.resolveUserId(authentication);
        NotificationResponse updated = notificationService.markAsRead(id, userId);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Mark all notifications as read", description = "Flags all notifications for the authenticated user as read.")
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        Long userId = securityUtils.resolveUserId(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
}
