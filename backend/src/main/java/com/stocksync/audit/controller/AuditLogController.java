package com.stocksync.audit.controller;

import com.stocksync.audit.entity.UserActivityLog;
import com.stocksync.audit.service.UserActivityLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/audit-logs")
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AuditLogController {

    private final UserActivityLogService auditLogService;

    public AuditLogController(UserActivityLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ResponseEntity<Page<UserActivityLog>> getAuditLogs(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            Pageable pageable) {
        return ResponseEntity.ok(auditLogService.searchLogs(userId, username, action, from, to, pageable));
    }
}
