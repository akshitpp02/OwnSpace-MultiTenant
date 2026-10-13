package com.saas.platform.restaurant;

import jakarta.validation.constraints.NotBlank;

public record PaymentRequest(@NotBlank(message = "Payment mode is required") String paymentMode) {
}
