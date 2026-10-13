package com.saas.platform.restaurant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MenuItemRequest(
    @NotNull(message = "Category is required") Long categoryId,
    @NotBlank(message = "Menu item name is required") @Size(max = 150) String name,
    @Size(max = 300) String description,
    @NotNull(message = "Price is required") @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero") BigDecimal price
) {
}
