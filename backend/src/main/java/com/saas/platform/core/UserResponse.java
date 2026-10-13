package com.saas.platform.core;

public record UserResponse(Long id, String name, String email, String role, boolean active) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.isActive());
    }
}
