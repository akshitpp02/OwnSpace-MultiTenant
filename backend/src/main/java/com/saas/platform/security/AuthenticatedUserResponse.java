package com.saas.platform.security;

public record AuthenticatedUserResponse(
    Long id,
    String name,
    String email,
    String role,
    String businessType,
    String businessName
) {
}
