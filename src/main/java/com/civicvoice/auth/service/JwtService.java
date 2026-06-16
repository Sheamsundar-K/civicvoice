package com.civicvoice.auth.service;

import com.civicvoice.config.AppProperties;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Handles JWT creation, validation, and Redis-backed token blacklisting.
 * Tokens are stateless but can be revoked on logout via Redis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final AppProperties appProperties;
    private final Map<String, Long> blacklistedTokens = new ConcurrentHashMap<>();

    // ─── Token Generation ────────────────────────────────────────────────────

    public String generateAccessToken(UserDetails userDetails) {
        return generateToken(userDetails, Map.of(), appProperties.getJwt().getExpirationMs());
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return generateToken(userDetails, Map.of("type", "refresh"),
                appProperties.getJwt().getRefreshExpirationMs());
    }

    private String generateToken(UserDetails userDetails, Map<String, Object> extraClaims, long expirationMs) {
        return Jwts.builder()
            .claims(extraClaims)
            .subject(userDetails.getUsername())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact();
    }

    // ─── Validation ──────────────────────────────────────────────────────────

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            if (isTokenBlacklisted(token)) {
                log.debug("Token is blacklisted");
                return false;
            }
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public Date extractExpiration(String token) {
        return getClaims(token).getExpiration();
    }

    // ─── Token Blacklisting (Logout) ─────────────────────────────────────────

    public void blacklistToken(String token) {
        Date expiration = extractExpiration(token);
        long ttl = expiration.getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            blacklistedTokens.put(token, expiration.getTime());
            log.info("Token blacklisted, TTL: {}ms", ttl);
        }
    }

    public boolean isTokenBlacklisted(String token) {
        Long expiration = blacklistedTokens.get(token);
        if (expiration != null) {
            if (expiration < System.currentTimeMillis()) {
                blacklistedTokens.remove(token);
                return false;
            }
            return true;
        }
        return false;
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = appProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
