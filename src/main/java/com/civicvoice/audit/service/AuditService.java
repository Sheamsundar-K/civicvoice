package com.civicvoice.audit.service;

import com.civicvoice.audit.domain.AuditLog;
import com.civicvoice.audit.repository.AuditLogRepository;
import com.civicvoice.issue.domain.Issue;
import com.civicvoice.user.domain.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Log an action to the audit trail. Uses Propagation.REQUIRES_NEW to ensure
     * that audit logs are written even if the calling transaction rolls back.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, UUID entityId, Object oldValue, Object newValue, User actor) {
        try {
            String oldValStr = serializeValue(oldValue);
            String newValStr = serializeValue(newValue);

            AuditLog auditLog = AuditLog.builder()
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValue(oldValStr)
                    .newValue(newValStr)
                    .actor(actor)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit logged: {} on {}/{} by {}", action, entityType, entityId, actor != null ? actor.getEmail() : "SYSTEM");
        } catch (Exception e) {
            log.error("Failed to write audit log for action: {}, entityType: {}, entityId: {}", action, entityType, entityId, e);
        }
    }

    public Page<AuditLog> getLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    public Page<AuditLog> getLogsByEntityType(String entityType, Pageable pageable) {
        return auditLogRepository.findByEntityType(entityType, pageable);
    }

    public Page<AuditLog> getLogsByEntityId(UUID entityId, Pageable pageable) {
        return auditLogRepository.findByEntityId(entityId, pageable);
    }

    private String serializeValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum) {
            return String.valueOf(value);
        }
        if (value instanceof UUID) {
            return value.toString();
        }

        try {
            // Avoid circular dependencies and lazy initialization issues on entity serialization
            if (value instanceof Issue issue) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", issue.getId());
                map.put("title", issue.getTitle());
                map.put("category", issue.getCategory() != null ? issue.getCategory().name() : null);
                map.put("priority", issue.getPriority() != null ? issue.getPriority().name() : null);
                map.put("status", issue.getStatus() != null ? issue.getStatus().name() : null);
                map.put("address", issue.getAddress());
                map.put("city", issue.getCity());
                map.put("slaDeadline", issue.getSlaDeadline());
                return objectMapper.writeValueAsString(map);
            }
            if (value instanceof User user) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", user.getId());
                map.put("email", user.getEmail());
                map.put("fullName", user.getFullName());
                map.put("role", user.getRole() != null ? user.getRole().name() : null);
                map.put("department", user.getDepartment());
                return objectMapper.writeValueAsString(map);
            }

            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("Failed to serialize audit log value, falling back to toString: {}", e.getMessage());
            return String.valueOf(value);
        }
    }
}
