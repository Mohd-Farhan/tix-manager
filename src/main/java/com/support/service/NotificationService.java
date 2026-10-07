package com.support.service;

import com.support.dto.NotificationResponse;
import com.support.entity.Notification;
import com.support.exception.ResourceNotFoundException;
import com.support.mapper.NotificationMapper;
import com.support.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ==============================================================================================
 * SERVICE: NotificationService (In-App Alert Queries & Mutation)
 * ==============================================================================================
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    @Autowired
    public NotificationService(NotificationRepository notificationRepository, NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.notificationMapper = notificationMapper;
    }

    public List<NotificationResponse> getNotificationsForUser(Long userId) {
        List<Notification> list = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId);
        return notificationMapper.toResponseList(list);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getRecipient().getId().equals(userId)) {
            throw new IllegalArgumentException("Cannot update notification belonging to another user.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        return notificationMapper.toResponse(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByRecipientId(userId);
        log.info("Marked all notifications as read for user id={}", userId);
    }
}
