package com.saas.platform.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "local-test-secret-that-is-at-least-32-bytes-long";

    @Test
    void tenantTokenContainsTenantIdentityClaims() {
        JwtService jwtService = new JwtService(SECRET);

        AuthUserPrincipal principal = jwtService.parse(
            jwtService.createTenantToken(7L, 12L, "OWNER", "RESTAURANT", "owner@example.com")
        );

        assertEquals(7L, principal.userId());
        assertEquals(12L, principal.tenantId());
        assertEquals("OWNER", principal.role());
        assertEquals("RESTAURANT", principal.businessType());
        assertEquals("owner@example.com", principal.email());
    }

    @Test
    void platformTokenHasNoTenantClaims() {
        JwtService jwtService = new JwtService(SECRET);

        AuthUserPrincipal principal = jwtService.parse(
            jwtService.createPlatformAdminToken(3L, "admin@example.com")
        );

        assertEquals(3L, principal.userId());
        assertEquals("PLATFORM_ADMIN", principal.role());
        assertEquals("admin@example.com", principal.email());
        assertEquals(null, principal.tenantId());
        assertEquals(null, principal.businessType());
    }

    @Test
    void rejectsSecretShorterThanThirtyTwoBytes() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("too-short"));
    }

    @Test
    void rejectsTamperedToken() {
        JwtService jwtService = new JwtService(SECRET);
        String token = jwtService.createTenantToken(7L, 12L, "OWNER", "RESTAURANT", "owner@example.com");
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThrows(JwtException.class, () -> jwtService.parse(tampered));
    }
}
