package com.saas.platform.restaurant;

import java.math.BigDecimal;
import java.util.List;

public record RestaurantDashboardResponse(
    BigDecimal todaysSales,
    long todaysOrderCount,
    long activeOrders,
    List<TopDishResponse> topDishes,
    List<OrdersPerHourResponse> ordersPerHour
) {
}
