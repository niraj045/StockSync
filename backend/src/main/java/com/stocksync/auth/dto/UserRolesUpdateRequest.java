package com.stocksync.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record UserRolesUpdateRequest(
        @NotEmpty(message = "At least one role is required")
        Set<String> roles,

        @NotNull(message = "Version is required for optimistic locking")
        Long version
) {}
