package com.civicvoice.auth.dto;

import com.civicvoice.user.domain.Role;
import java.util.UUID;

public sealed interface AuthResponse permits AuthResponse.TokenPair, AuthResponse.UserInfo {

    record TokenPair(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        UserInfo user
    ) implements AuthResponse {}

    record UserInfo(
        UUID id,
        String email,
        String fullName,
        Role role,
        String department,
        String ward
    ) implements AuthResponse {}
}
