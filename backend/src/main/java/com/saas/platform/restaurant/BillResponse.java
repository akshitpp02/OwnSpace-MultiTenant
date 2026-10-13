package com.saas.platform.restaurant;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BillResponse(
    Long id,
    Long orderId,
    String tableName,
    String cashierName,
    BigDecimal subtotal,
    BigDecimal tax,
    BigDecimal total,
    String paymentMode,
    LocalDateTime paidAt,
    List<OrderItemResponse> items
) {
    static BillResponse from(Bill bill, List<OrderItem> items) {
        return new BillResponse(
            bill.getId(),
            bill.getOrder().getId(),
            bill.getOrder().getDiningTable().getName(),
            bill.getCashier().getName(),
            bill.getSubtotal(),
            bill.getTax(),
            bill.getTotal(),
            bill.getPaymentMode(),
            bill.getPaidAt(),
            items.stream().map(OrderItemResponse::from).toList()
        );
    }
}
