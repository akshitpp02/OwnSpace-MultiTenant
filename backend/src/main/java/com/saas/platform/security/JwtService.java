package com.saas.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final long TOKEN_LIFETIME_MILLIS = 8 * 60 * 60 * 1000L;

    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("JWT_SECRET must be at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createTenantToken(Long userId, Long tenantId, String role, String businessType, String email) {
        return createToken(userId, tenantId, role, businessType, email);
    }

    public String createPlatformAdminToken(Long userId, String email) {
        return createToken(userId, null, "PLATFORM_ADMIN", null, email);
    }

    public AuthUserPrincipal parse(String token) throws JwtException {
        Claims claims = Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();

        Long userId = requiredLong(claims, "userId");
        String role = requiredString(claims, "role");
        String email = claims.getSubject();
        if (email == null || email.isBlank()) {
            throw new JwtException("Token subject is missing");
        }

        if ("PLATFORM_ADMIN".equals(role)) {
            if (claims.containsKey("tenantId") || claims.containsKey("businessType")) {
                throw new JwtException("Platform token contains tenant claims");
            }
            return new AuthUserPrincipal(userId, null, email, role, null);
        }

        Long tenantId = requiredLong(claims, "tenantId");
        String businessType = requiredString(claims, "businessType");
        return new AuthUserPrincipal(userId, tenantId, email, role, businessType);
    }

    private String createToken(Long userId, Long tenantId, String role, String businessType, String email) {
        Instant now = Instant.now();
        var builder = Jwts.builder()
            .subject(email)
            .claim("userId", userId)
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(TOKEN_LIFETIME_MILLIS)));
        if (tenantId != null) {
            builder.claim("tenantId", tenantId)
                .claim("businessType", businessType);
        }
        return builder.signWith(signingKey).compact();
    }

    private Long requiredLong(Claims claims, String name) {
        Object value = claims.get(name);
        if (!(value instanceof Number number) || number.longValue() <= 0) {
            throw new JwtException("Token claim is invalid: " + name);
        }
        return number.longValue();
    }

    private String requiredString(Claims claims, String name) {
        String value = claims.get(name, String.class);
        if (value == null || value.isBlank()) {
            throw new JwtException("Token claim is invalid: " + name);
        }
        return value;
    }
}
