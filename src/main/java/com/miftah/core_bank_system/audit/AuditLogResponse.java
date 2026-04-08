package com.miftah.core_bank_system.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuditLogResponse {
    private UUID id;
    private UUID userId;
    private AuditAction action;
    private String details;
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;
}
