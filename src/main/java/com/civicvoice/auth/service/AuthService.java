package com.civicvoice.auth.service;

import com.civicvoice.auth.dto.AuthRequest;
import com.civicvoice.auth.dto.AuthResponse;
import com.civicvoice.common.exception.BusinessRuleException;
import com.civicvoice.common.exception.DuplicateResourceException;
import com.civicvoice.user.domain.Role;
import com.civicvoice.user.domain.User;
import com.civicvoice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse.TokenPair register(AuthRequest.Register request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        User user = User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .fullName(request.fullName())
            .phone(request.phone())
            .role(Role.CITIZEN)
            .isActive(true)
            .isVerified(false)
            .build();

        userRepository.save(user);
        log.info("New citizen registered: {}", user.getEmail());
        return buildTokenPair(user);
    }

    @Transactional
    public AuthResponse.TokenPair registerAuthority(AuthRequest.AuthorityRegister request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        User user = User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .fullName(request.fullName())
            .phone(request.phone())
            .role(Role.AUTHORITY)
            .department(request.department())
            .ward(request.ward())
            .isActive(true)
            .isVerified(true)
            .build();

        userRepository.save(user);
        log.info("Authority account created: {} (dept: {})", user.getEmail(), user.getDepartment());
        return buildTokenPair(user);
    }

    @Transactional
    public AuthResponse.TokenPair registerNgo(AuthRequest.NgoRegister request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email already registered: " + request.email());
        }

        User user = User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .fullName(request.fullName())
            .phone(request.phone())
            .role(Role.NGO)
            .department(request.department())
            .ward(request.ward())
            .isActive(true)
            .isVerified(true)
            .build();

        userRepository.save(user);
        log.info("NGO account created: {} (org: {})", user.getEmail(), user.getDepartment());
        return buildTokenPair(user);
    }

    public AuthResponse.TokenPair login(AuthRequest.Login request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!user.isActive()) {
            throw new BusinessRuleException("Account is deactivated. Contact support.");
        }

        log.info("User logged in: {}", user.getEmail());
        return buildTokenPair(user);
    }

    public AuthResponse.TokenPair refresh(AuthRequest.RefreshToken request) {
        String username = jwtService.extractUsername(request.refreshToken());
        User user = userRepository.findByEmail(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!jwtService.isTokenValid(request.refreshToken(), user)) {
            throw new BusinessRuleException("Invalid or expired refresh token");
        }

        // Blacklist old refresh token
        jwtService.blacklistToken(request.refreshToken());
        return buildTokenPair(user);
    }

    public void logout(String token) {
        jwtService.blacklistToken(token);
        log.info("Token revoked on logout");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private AuthResponse.TokenPair buildTokenPair(User user) {
        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
            user.getId(), user.getEmail(), user.getFullName(),
            user.getRole(), user.getDepartment(), user.getWard()
        );

        return new AuthResponse.TokenPair(
            accessToken, refreshToken,
            jwtService.extractExpiration(accessToken).getTime(),
            "Bearer", userInfo
        );
    }
}
