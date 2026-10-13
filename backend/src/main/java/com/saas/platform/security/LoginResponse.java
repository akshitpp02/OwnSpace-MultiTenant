package com.saas.platform.security;

public record LoginResponse(String token, AuthenticatedUserResponse user) {
}
