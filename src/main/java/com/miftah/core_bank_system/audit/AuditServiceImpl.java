package com.miftah.core_bank_system.audit;

import com.miftah.core_bank_system.user.User;
import com.miftah.core_bank_system.exception.ResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditServiceImpl implements AuditService {

    private final AuditRepository auditRepository;

    @Override
    @Transactional
    public void logAction(User user, AuditAction action, String details) {
        log.info("Logging audit action: {} for user: {}", action, user.getUsername());
        
        String ipAddress = "UNKNOWN";
        String userAgent = "UNKNOWN";
        
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                    ipAddress = request.getRemoteAddr();
                }
                userAgent = request.getHeader("User-Agent");
                if (userAgent == null) {
                    userAgent = "UNKNOWN";
                }
            }
        } catch (Exception e) {
            log.warn("Could not retrieve HTTP request attributes for audit log", e);
        }

        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .action(action)
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
                
        auditRepository.save(auditLog);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getLogs(UUID userId, AuditAction action, Instant startDate, Instant endDate, Pageable pageable) {
        Specification<AuditLog> spec = buildSpecification(userId, action, startDate, endDate);
        return auditRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getMyLogs(User user, AuditAction action, Instant startDate, Instant endDate, Pageable pageable) {
        Specification<AuditLog> spec = buildSpecification(user.getId(), action, startDate, endDate);
        return auditRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AuditLogResponse getLogById(UUID id) {
        AuditLog auditLog = auditRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog", id));
        return toResponse(auditLog);
    }

    private Specification<AuditLog> buildSpecification(UUID userId, AuditAction action, Instant startDate, Instant endDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), action));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            }
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .userId(auditLog.getUser().getId())
                .action(auditLog.getAction())
                .details(auditLog.getDetails())
                .ipAddress(auditLog.getIpAddress())
                .userAgent(auditLog.getUserAgent())
                .createdAt(auditLog.getCreatedAt())
                .build();
    }
}
