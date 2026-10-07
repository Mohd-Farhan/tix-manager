package com.support.entity;

/**
 * ==============================================================================================
 * ENUM: NotificationType
 * ==============================================================================================
 *
 * Categorizes the type of notification dispatched by the Observer Pattern.
 */
public enum NotificationType {
    TICKET_ASSIGNED,
    STATUS_CHANGED,
    NEW_MESSAGE,
    SLA_BREACH
}
