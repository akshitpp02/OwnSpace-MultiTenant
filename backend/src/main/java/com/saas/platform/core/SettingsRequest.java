package com.saas.platform.core;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record SettingsRequest(
    @NotBlank(message = "Business name is required")
    @Size(max = 150, message = "Business name cannot exceed 150 characters")
    String businessName,
    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    String phone,
    @Size(max = 250, message = "Address cannot exceed 250 characters")
    String address,
    @DecimalMin(value = "0.0", message = "Tax percent cannot be negative")
    @DecimalMax(value = "100.0", message = "Tax percent cannot exceed 100")
    BigDecimal taxPercent
) {
}
