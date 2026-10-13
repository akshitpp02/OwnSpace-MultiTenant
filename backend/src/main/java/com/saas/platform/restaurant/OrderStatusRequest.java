package com.saas.platform.restaurant;

import jakarta.validation.constraints.NotBlank;

public record OrderStatusRequest(@NotBlank(message = "Order status is required") String status) {
}
