package com.saas.platform.security;

public record AuthUserPrincipal(
    Long userId,
    Long tenantId,
    String email,
    String role,
    String businessType
) {
}
