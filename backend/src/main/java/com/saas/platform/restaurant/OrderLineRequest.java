package com.saas.platform.restaurant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderLineRequest(
    @NotNull(message = "Menu item is required") Long menuItemId,
    @Min(value = 1, message = "Quantity must be at least one") int quantity,
    @Size(max = 200, message = "Notes cannot exceed 200 characters") String notes
) {
}
