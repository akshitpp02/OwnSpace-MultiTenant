package com.saas.platform.restaurant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AddOrderItemsRequest(@NotEmpty(message = "At least one item is required") List<@Valid OrderLineRequest> items) {
}
