package com.miftah.core_bank_system.notification;

import com.miftah.core_bank_system.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {
    Page<NotificationResponse> getMyNotifications(User user, Pageable pageable);
    long getUnreadCount(User user);
    void markAsRead(User user, UUID notificationId);
    void markAllAsRead(User user);
}
