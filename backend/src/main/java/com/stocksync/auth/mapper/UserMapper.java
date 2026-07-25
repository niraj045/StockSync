package com.stocksync.auth.mapper;

import com.stocksync.auth.dto.UserResponse;
import com.stocksync.auth.entity.Role;
import com.stocksync.auth.entity.User;

import java.util.stream.Collectors;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getUsername(),
                user.getEmail(),
                user.isActive(),
                user.getLastLoginAt(),
                user.getCreatedAt(),
                user.getCreatedBy(),
                user.getUpdatedAt(),
                user.getUpdatedBy(),
                user.getVersion(),
                user.getRoles().stream().map(Role::getId).collect(Collectors.toSet())
        );
    }
}
