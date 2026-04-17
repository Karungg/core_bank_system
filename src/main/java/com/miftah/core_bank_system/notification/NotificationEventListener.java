package com.miftah.core_bank_system.notification;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.miftah.core_bank_system.notification.event.AccountStatusChangedEvent;
import com.miftah.core_bank_system.notification.event.LoginEvent;
import com.miftah.core_bank_system.notification.event.PinChangedEvent;
import com.miftah.core_bank_system.notification.event.TransactionCompletedEvent;
import com.miftah.core_bank_system.user.User;
import com.miftah.core_bank_system.user.UserRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @EventListener
    @Async("notificationExecutor")
    @Transactional
    public void handleTransactionCompleted(TransactionCompletedEvent event) {
        String title = switch (event.getType()) {
            case TRANSFER -> "Transfer Berhasil";
            case DEPOSIT -> "Deposit Diterima";
            case WITHDRAWAL -> "Penarikan Berhasil";
        };
        String message = String.format("Transaksi %s sebesar Rp %s berhasil diproses.",
                event.getType(), event.getAmount());

        saveNotification(event.getUserId(), title, message, NotificationType.TRANSACTION);
    }

    @EventListener
    @Async("notificationExecutor")
    @Transactional
    public void handleLogin(LoginEvent event) {
        if (event.isSuccess()) {
            saveNotification(event.getUserId(),
                "Login Baru",
                "Login terdeteksi dari IP: " + event.getIpAddress(),
                NotificationType.SECURITY);
        } else {
            saveNotification(event.getUserId(),
                "Aktivitas Mencurigakan",
                "Percobaan login gagal terdeteksi dari IP: " + event.getIpAddress(),
                NotificationType.SECURITY);
        }
    }

    @EventListener
    @Async("notificationExecutor")
    @Transactional
    public void handleAccountStatusChanged(AccountStatusChangedEvent event) {
        saveNotification(event.getUserId(),
            "Status Akun Diubah",
            String.format("Status akun Anda berubah dari %s menjadi %s.", event.getOldStatus(), event.getNewStatus()),
            NotificationType.ACCOUNT);
    }

    @EventListener
    @Async("notificationExecutor")
    @Transactional
    public void handlePinChanged(PinChangedEvent event) {
        saveNotification(event.getUserId(),
            "PIN Berhasil Diubah",
            "PIN akun Anda telah berhasil diperbarui.",
            NotificationType.SECURITY);
    }

    private void saveNotification(UUID userId, String title, String message, NotificationType type) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .read(false)
                .build();
        
        notificationRepository.save(notification);
        log.info("Saved notification for user {}: {}", user.getUsername(), title);
    }
}
