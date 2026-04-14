package com.miftah.core_bank_system.notification;

import com.miftah.core_bank_system.dto.WebResponse;
import com.miftah.core_bank_system.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final MessageSource messageSource;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<Page<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal User user,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<NotificationResponse> responses = notificationService.getMyNotifications(user, pageable);
        String message = messageSource.getMessage("success.get", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, responses)
        );
    }

    @GetMapping(path = "/unread-count", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<Long>> getUnreadCount(@AuthenticationPrincipal User user) {
        long count = notificationService.getUnreadCount(user);
        String message = messageSource.getMessage("success.get", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, count)
        );
    }

    @PatchMapping(path = "/{id}/read", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<String>> markAsRead(
            @AuthenticationPrincipal User user,
            @PathVariable("id") UUID id) {
        
        notificationService.markAsRead(user, id);
        String message = messageSource.getMessage("success.update", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, "OK")
        );
    }

    @PatchMapping(path = "/read-all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<String>> markAllAsRead(@AuthenticationPrincipal User user) {
        notificationService.markAllAsRead(user);
        String message = messageSource.getMessage("success.update", null, LocaleContextHolder.getLocale());
        
        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, "OK")
        );
    }
}
