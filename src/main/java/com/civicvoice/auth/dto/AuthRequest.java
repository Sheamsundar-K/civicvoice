package com.civicvoice.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public sealed interface AuthRequest permits AuthRequest.Register, AuthRequest.Login,
        AuthRequest.AuthorityRegister, AuthRequest.NgoRegister, AuthRequest.RefreshToken {

    record Register(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank String fullName,
        String phone
    ) implements AuthRequest {}

    record Login(
        @NotBlank @Email String email,
        @NotBlank String password
    ) implements AuthRequest {}

    record AuthorityRegister(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank String fullName,
        String phone,
        @NotBlank String department,
        @NotBlank String ward
    ) implements AuthRequest {}

    record NgoRegister(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank String fullName,
        String phone,
        @NotBlank String department, // Reusing department to store NGO details/organization
        @NotBlank String ward
    ) implements AuthRequest {}

    record RefreshToken(
        @NotBlank String refreshToken
    ) implements AuthRequest {}
}
