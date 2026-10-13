package com.saas.platform.security;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TenantLoginRequest(
    @NotBlank(message = "Business code is required") String businessCode,
    @NotBlank(message = "Email is required") @Email(message = "Email is invalid") String email,
    @NotBlank(message = "Password is required") String password
) {
}
