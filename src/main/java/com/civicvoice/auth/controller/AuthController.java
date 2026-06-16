package com.civicvoice.auth.controller;

import com.civicvoice.auth.dto.AuthRequest;
import com.civicvoice.auth.dto.AuthResponse;
import com.civicvoice.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, refresh, and logout")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new citizen account")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse.TokenPair> register(
            @Valid @RequestBody AuthRequest.Register request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Login and receive JWT tokens")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse.TokenPair> login(
            @Valid @RequestBody AuthRequest.Login request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Operation(summary = "Refresh access token using refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse.TokenPair> refresh(
            @Valid @RequestBody AuthRequest.RefreshToken request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @Operation(summary = "Logout – revoke current token",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            authService.logout(token);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @Operation(summary = "Create authority account (ADMIN only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/authority/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponse.TokenPair> registerAuthority(
            @Valid @RequestBody AuthRequest.AuthorityRegister request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerAuthority(request));
    }

    @Operation(summary = "Create NGO account (ADMIN only)",
               security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/ngo/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponse.TokenPair> registerNgo(
            @Valid @RequestBody AuthRequest.NgoRegister request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerNgo(request));
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
