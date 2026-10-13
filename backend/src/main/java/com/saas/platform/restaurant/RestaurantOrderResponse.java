package com.saas.platform.restaurant;

import java.time.LocalDateTime;
import java.util.List;

public record RestaurantOrderResponse(
    Long id,
    Long tableId,
    String tableName,
    Long waiterId,
    String waiterName,
    String status,
    LocalDateTime createdAt,
    List<OrderItemResponse> items
) {
    static RestaurantOrderResponse from(RestaurantOrder order, List<OrderItem> items) {
        return new RestaurantOrderResponse(
            order.getId(),
            order.getDiningTable().getId(),
            order.getDiningTable().getName(),
            order.getWaiter().getId(),
            order.getWaiter().getName(),
            order.getStatus(),
            order.getCreatedAt(),
            items.stream().map(OrderItemResponse::from).toList()
        );
    }
}
