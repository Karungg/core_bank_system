package com.miftah.core_bank_system.audit;

import com.miftah.core_bank_system.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface AuditService {
    void logAction(User user, AuditAction action, String details);
    Page<AuditLogResponse> getLogs(UUID userId, AuditAction action, Instant startDate, Instant endDate, Pageable pageable);
    Page<AuditLogResponse> getMyLogs(User user, AuditAction action, Instant startDate, Instant endDate, Pageable pageable);
    AuditLogResponse getLogById(UUID id);
}
