package com.miftah.core_bank_system.audit;

import com.miftah.core_bank_system.dto.WebResponse;
import com.miftah.core_bank_system.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;
    private final MessageSource messageSource;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<Page<AuditLogResponse>>> getLogs(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        Page<AuditLogResponse> response = auditService.getLogs(userId, action, startDate, endDate, pageable);
        String message = messageSource.getMessage("success.get", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, response)
        );
    }

    @GetMapping(path = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<Page<AuditLogResponse>>> getMyLogs(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Instant startDate,
            @RequestParam(required = false) Instant endDate,
            @PageableDefault(size = 10, sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {

        Page<AuditLogResponse> response = auditService.getMyLogs(user, action, startDate, endDate, pageable);
        String message = messageSource.getMessage("success.get", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, response)
        );
    }

    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<WebResponse<AuditLogResponse>> getLogById(@PathVariable("id") UUID id) {
        AuditLogResponse response = auditService.getLogById(id);
        String message = messageSource.getMessage("success.get", null, LocaleContextHolder.getLocale());

        return ResponseEntity.status(HttpStatus.OK).body(
                WebResponse.success(HttpStatus.OK.value(), message, response)
        );
    }
}
