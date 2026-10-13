package com.saas.platform.restaurant;

import java.math.BigDecimal;

public record MenuItemResponse(
    Long id,
    Long categoryId,
    String categoryName,
    String name,
    String description,
    BigDecimal price,
    boolean available
) {
    static MenuItemResponse from(MenuItem item) {
        return new MenuItemResponse(
            item.getId(),
            item.getCategory().getId(),
            item.getCategory().getName(),
            item.getName(),
            item.getDescription(),
            item.getPrice(),
            item.isAvailable()
        );
    }
}
