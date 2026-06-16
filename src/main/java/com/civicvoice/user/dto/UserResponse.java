package com.civicvoice.user.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String name,
    String email,
    String role,
    OffsetDateTime createdAt,
    boolean isActive
) {
    public static UserResponse from(com.civicvoice.user.domain.User user) {
        return new UserResponse(
            user.getId(),
            user.getFullName(),
            user.getEmail(),
            user.getRole().name(),
            user.getCreatedAt(),
            user.isActive()
        );
    }
}
