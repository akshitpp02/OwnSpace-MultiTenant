package com.saas.platform.core;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 120, message = "Name cannot exceed 120 characters")
    String name,
    @NotBlank(message = "Email is required")
    @Email(message = "Email is invalid")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    String email,
    @NotBlank(message = "Role is required")
    String role,
    String password
) {
}
