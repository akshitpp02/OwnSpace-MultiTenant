package com.saas.platform.platform;

import jakarta.validation.constraints.NotBlank;

public record TenantStatusRequest(
    @NotBlank(message = "Tenant status is required") String status
) {
}
