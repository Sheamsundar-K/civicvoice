package com.civicvoice.audit.repository;

import com.civicvoice.audit.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByEntityType(String entityType, Pageable pageable);
    Page<AuditLog> findByEntityId(UUID entityId, Pageable pageable);
}
