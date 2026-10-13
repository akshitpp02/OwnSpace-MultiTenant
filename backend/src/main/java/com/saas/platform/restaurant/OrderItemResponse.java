package com.saas.platform.restaurant;

import java.math.BigDecimal;

public record OrderItemResponse(
    Long id,
    Long menuItemId,
    String name,
    int quantity,
    BigDecimal unitPrice,
    BigDecimal lineTotal,
    String notes
) {
    static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
            item.getId(),
            item.getMenuItem().getId(),
            item.getMenuItem().getName(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())),
            item.getNotes()
        );
    }
}
