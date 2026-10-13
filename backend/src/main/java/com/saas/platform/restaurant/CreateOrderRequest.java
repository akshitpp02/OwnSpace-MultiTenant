package com.saas.platform.restaurant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateOrderRequest(
    @NotNull(message = "Dining table is required") Long tableId,
    @NotEmpty(message = "An order must contain at least one item") List<@Valid OrderLineRequest> items
) {
}
