package com.saas.platform.retail;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

record RetailCategoryRequest(
    @NotBlank(message = "Category name is required") @Size(max = 100) String name
) {}

record RetailCategoryResponse(Long id, String name) {
    static RetailCategoryResponse from(Category category) {
        return new RetailCategoryResponse(category.getId(), category.getName());
    }
}

record ProductRequest(
    Long categoryId,
    @NotBlank(message = "Product name is required") @Size(max = 150) String name,
    @NotBlank(message = "SKU is required") @Size(max = 50) String sku,
    @DecimalMin(value = "0.0", message = "Price cannot be negative") BigDecimal price,
    @DecimalMin(value = "0.0", message = "Cost price cannot be negative") BigDecimal costPrice,
    @Min(value = 0, message = "Stock cannot be negative") Integer stockQty,
    @Min(value = 0, message = "Low-stock threshold cannot be negative") Integer lowStockThreshold,
    Boolean active
) {}

record ProductResponse(
    Long id, Long categoryId, String categoryName, String name, String sku,
    BigDecimal price, BigDecimal costPrice, int stockQty, int lowStockThreshold,
    boolean active, boolean lowStock
) {
    static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getCategory() == null ? null : product.getCategory().getId(),
            product.getCategory() == null ? null : product.getCategory().getName(),
            product.getName(),
            product.getSku(),
            product.getPrice(),
            product.getCostPrice(),
            product.getStockQty(),
            product.getLowStockThreshold(),
            product.isActive(),
            product.getStockQty() <= product.getLowStockThreshold()
        );
    }
}

record ActiveRequest(@NotNull Boolean active) {}

record CustomerRequest(
    @NotBlank(message = "Customer name is required") @Size(max = 120) String name,
    @NotBlank(message = "Phone number is required") @Size(max = 20) String phone,
    @Email(message = "Enter a valid email address") @Size(max = 150) String email,
    @Size(max = 250) String address
) {}

record CustomerResponse(Long id, String name, String phone, String email, String address) {
    static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.getId(), customer.getName(), customer.getPhone(), customer.getEmail(), customer.getAddress());
    }
}

record SupplierRequest(
    @NotBlank(message = "Supplier name is required") @Size(max = 150) String name,
    @Size(max = 20) String phone,
    @Size(max = 250) String address
) {}

record SupplierResponse(Long id, String name, String phone, String address) {
    static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(supplier.getId(), supplier.getName(), supplier.getPhone(), supplier.getAddress());
    }
}

record SaleLineRequest(
    @NotNull(message = "Product is required") Long productId,
    @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least one") Integer quantity
) {}

record SaleRequest(
    @NotEmpty(message = "A sale must contain at least one item") List<@Valid SaleLineRequest> items,
    Long customerId,
    @DecimalMin(value = "0.0", message = "Discount cannot be negative") BigDecimal discount,
    @NotBlank(message = "Payment mode is required") @Pattern(regexp = "(?i)CASH|CARD|UPI", message = "Payment mode must be CASH, CARD, or UPI") String paymentMode
) {}

record SaleItemResponse(
    Long id, Long productId, String productName, String sku, int quantity,
    BigDecimal unitPrice, BigDecimal unitCost, BigDecimal lineTotal
) {
    static SaleItemResponse from(SaleItem item) {
        return new SaleItemResponse(item.getId(), item.getProduct().getId(), item.getProduct().getName(),
            item.getProduct().getSku(), item.getQuantity(), item.getUnitPrice(), item.getUnitCost(),
            item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }
}

record SaleResponse(
    Long id, Long customerId, String customerName, Long cashierId, String cashierName,
    BigDecimal subtotal, BigDecimal discount, BigDecimal tax, BigDecimal total,
    String paymentMode, LocalDateTime createdAt, List<SaleItemResponse> items
) {
    static SaleResponse from(Sale sale, List<SaleItem> items) {
        return new SaleResponse(sale.getId(),
            sale.getCustomer() == null ? null : sale.getCustomer().getId(),
            sale.getCustomer() == null ? null : sale.getCustomer().getName(),
            sale.getCashier().getId(), sale.getCashier().getName(), sale.getSubtotal(),
            sale.getDiscount(), sale.getTax(), sale.getTotal(), sale.getPaymentMode(),
            sale.getCreatedAt(), items.stream().map(SaleItemResponse::from).toList());
    }
}

record PurchaseLineRequest(
    @NotNull(message = "Product is required") Long productId,
    @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be at least one") Integer quantity,
    @NotNull(message = "Unit cost is required") @DecimalMin(value = "0.0", message = "Unit cost cannot be negative") BigDecimal unitCost
) {}

record PurchaseRequest(
    @NotNull(message = "Supplier is required") Long supplierId,
    @NotEmpty(message = "A purchase must contain at least one item") List<@Valid PurchaseLineRequest> items
) {}

record PurchaseItemResponse(Long id, Long productId, String productName, String sku, int quantity, BigDecimal unitCost) {
    static PurchaseItemResponse from(PurchaseItem item) {
        return new PurchaseItemResponse(item.getId(), item.getProduct().getId(), item.getProduct().getName(),
            item.getProduct().getSku(), item.getQuantity(), item.getUnitCost());
    }
}

record PurchaseResponse(
    Long id, Long supplierId, String supplierName, Long receivedById, String receivedByName,
    BigDecimal totalCost, LocalDateTime createdAt, List<PurchaseItemResponse> items
) {
    static PurchaseResponse from(Purchase purchase, List<PurchaseItem> items) {
        return new PurchaseResponse(purchase.getId(), purchase.getSupplier().getId(), purchase.getSupplier().getName(),
            purchase.getReceivedBy().getId(), purchase.getReceivedBy().getName(), purchase.getTotalCost(),
            purchase.getCreatedAt(), items.stream().map(PurchaseItemResponse::from).toList());
    }
}

record StockAdjustmentRequest(
    @NotNull(message = "Product is required") Long productId,
    @NotNull(message = "Quantity adjustment is required") Integer changeQty,
    @NotBlank(message = "An adjustment note is required") @Size(max = 200) String note
) {}

record StockMovementResponse(
    Long id, Long productId, String productName, String sku, int changeQty,
    String reason, Long referenceId, String note, Long createdById, String createdByName,
    LocalDateTime createdAt
) {
    static StockMovementResponse from(StockMovement movement) {
        return new StockMovementResponse(movement.getId(), movement.getProduct().getId(),
            movement.getProduct().getName(), movement.getProduct().getSku(), movement.getChangeQty(),
            movement.getReason(), movement.getReferenceId(), movement.getNote(), movement.getCreatedBy().getId(),
            movement.getCreatedBy().getName(), movement.getCreatedAt());
    }
}

record TopProductResponse(String name, String sku, long quantitySold) {}
record SalesPerDayResponse(LocalDate date, BigDecimal sales) {}
record RetailDashboardResponse(
    BigDecimal todaysSales,
    BigDecimal todaysProfit,
    long todaysBillCount,
    long lowStockProductCount,
    List<TopProductResponse> topProducts,
    List<SalesPerDayResponse> salesPerDay
) {}
