package com.stocksync.auth.dto;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String fullName,
        String username,
        String email,
        boolean active,
        Instant lastLoginAt,
        Instant createdAt,
        String createdBy,
        Instant updatedAt,
        String updatedBy,
        Long version,
        Set<String> roles
) {}
