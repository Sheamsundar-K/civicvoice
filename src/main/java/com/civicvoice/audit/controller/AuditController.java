package com.civicvoice.audit.controller;

import com.civicvoice.audit.domain.AuditLog;
import com.civicvoice.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public Page<AuditLog> getLogs(Pageable pageable) {
        return auditService.getLogs(pageable);
    }

    @GetMapping("/entity-type/{entityType}")
    public Page<AuditLog> getLogsByEntityType(@PathVariable String entityType, Pageable pageable) {
        return auditService.getLogsByEntityType(entityType, pageable);
    }

    @GetMapping("/entity/{entityId}")
    public Page<AuditLog> getLogsByEntityId(@PathVariable UUID entityId, Pageable pageable) {
        return auditService.getLogsByEntityId(entityId, pageable);
    }
}
