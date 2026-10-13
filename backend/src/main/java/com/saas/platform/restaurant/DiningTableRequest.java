package com.saas.platform.restaurant;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiningTableRequest(
    @NotBlank(message = "Table name is required") @Size(max = 50) String name,
    @Min(value = 1, message = "Table capacity must be at least one") int capacity
) {
}
