package com.saas.platform.retail;

import com.saas.platform.common.ApiResponse;
import com.saas.platform.common.PageResponse;
import com.saas.platform.security.AuthUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class RetailController {

    private final RetailService retailService;

    public RetailController(RetailService retailService) {
        this.retailService = retailService;
    }

    @GetMapping("/api/retail/categories")
    public ApiResponse<PageResponse<RetailCategoryResponse>> categories(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "100") @Min(1) @Max(100) int size,
        Authentication authentication) {
        return ApiResponse.success("Retail categories retrieved",
            retailService.listCategories(principal(authentication), page, size));
    }

    @PostMapping("/api/retail/categories")
    public ApiResponse<RetailCategoryResponse> createCategory(
        @Valid @RequestBody RetailCategoryRequest request, Authentication authentication) {
        return ApiResponse.success("Category created", retailService.createCategory(request, principal(authentication)));
    }

    @PutMapping("/api/retail/categories/{id}")
    public ApiResponse<RetailCategoryResponse> updateCategory(
        @PathVariable @Min(1) long id, @Valid @RequestBody RetailCategoryRequest request, Authentication authentication) {
        return ApiResponse.success("Category updated", retailService.updateCategory(id, request, principal(authentication)));
    }

    @DeleteMapping("/api/retail/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable @Min(1) long id, Authentication authentication) {
        retailService.deleteCategory(id, principal(authentication));
        return ApiResponse.success("Category deleted", null);
    }

    @GetMapping("/api/products")
    public ApiResponse<PageResponse<ProductResponse>> products(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) Boolean active,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        Authentication authentication) {
        return ApiResponse.success("Products retrieved",
            retailService.listProducts(search, categoryId, active, page, size, principal(authentication)));
    }

    @GetMapping("/api/products/low-stock")
    public ApiResponse<List<ProductResponse>> lowStock(Authentication authentication) {
        return ApiResponse.success("Low-stock products retrieved", retailService.lowStockProducts(principal(authentication)));
    }

    @PostMapping("/api/products")
    public ApiResponse<ProductResponse> createProduct(
        @Valid @RequestBody ProductRequest request, Authentication authentication) {
        return ApiResponse.success("Product created", retailService.createProduct(request, principal(authentication)));
    }

    @PutMapping("/api/products/{id}")
    public ApiResponse<ProductResponse> updateProduct(
        @PathVariable @Min(1) long id, @Valid @RequestBody ProductRequest request, Authentication authentication) {
        return ApiResponse.success("Product updated", retailService.updateProduct(id, request, principal(authentication)));
    }

    @PatchMapping("/api/products/{id}/active")
    public ApiResponse<ProductResponse> setProductActive(
        @PathVariable @Min(1) long id, @Valid @RequestBody ActiveRequest request, Authentication authentication) {
        return ApiResponse.success("Product status updated",
            retailService.setProductActive(id, request.active(), principal(authentication)));
    }

    @DeleteMapping("/api/products/{id}")
    public ApiResponse<Void> deleteProduct(@PathVariable @Min(1) long id, Authentication authentication) {
        retailService.deleteProduct(id, principal(authentication));
        return ApiResponse.success("Product deleted", null);
    }

    @GetMapping("/api/customers")
    public ApiResponse<PageResponse<CustomerResponse>> customers(
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        Authentication authentication) {
        return ApiResponse.success("Customers retrieved",
            retailService.listCustomers(search, page, size, principal(authentication)));
    }

    @PostMapping("/api/customers")
    public ApiResponse<CustomerResponse> createCustomer(
        @Valid @RequestBody CustomerRequest request, Authentication authentication) {
        return ApiResponse.success("Customer created", retailService.createCustomer(request, principal(authentication)));
    }

    @PutMapping("/api/customers/{id}")
    public ApiResponse<CustomerResponse> updateCustomer(
        @PathVariable @Min(1) long id, @Valid @RequestBody CustomerRequest request, Authentication authentication) {
        return ApiResponse.success("Customer updated", retailService.updateCustomer(id, request, principal(authentication)));
    }

    @DeleteMapping("/api/customers/{id}")
    public ApiResponse<Void> deleteCustomer(@PathVariable @Min(1) long id, Authentication authentication) {
        retailService.deleteCustomer(id, principal(authentication));
        return ApiResponse.success("Customer deleted", null);
    }

    @GetMapping("/api/suppliers")
    public ApiResponse<PageResponse<SupplierResponse>> suppliers(
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        Authentication authentication) {
        return ApiResponse.success("Suppliers retrieved",
            retailService.listSuppliers(search, page, size, principal(authentication)));
    }

    @PostMapping("/api/suppliers")
    public ApiResponse<SupplierResponse> createSupplier(
        @Valid @RequestBody SupplierRequest request, Authentication authentication) {
        return ApiResponse.success("Supplier created", retailService.createSupplier(request, principal(authentication)));
    }

    @PutMapping("/api/suppliers/{id}")
    public ApiResponse<SupplierResponse> updateSupplier(
        @PathVariable @Min(1) long id, @Valid @RequestBody SupplierRequest request, Authentication authentication) {
        return ApiResponse.success("Supplier updated", retailService.updateSupplier(id, request, principal(authentication)));
    }

    @DeleteMapping("/api/suppliers/{id}")
    public ApiResponse<Void> deleteSupplier(@PathVariable @Min(1) long id, Authentication authentication) {
        retailService.deleteSupplier(id, principal(authentication));
        return ApiResponse.success("Supplier deleted", null);
    }

    @PostMapping("/api/sales")
    public ApiResponse<SaleResponse> createSale(
        @Valid @RequestBody SaleRequest request, Authentication authentication) {
        return ApiResponse.success("Sale completed", retailService.createSale(request, principal(authentication)));
    }

    @GetMapping("/api/sales")
    public ApiResponse<PageResponse<SaleResponse>> sales(
        @RequestParam(required = false) LocalDate fromDate,
        @RequestParam(required = false) LocalDate toDate,
        @RequestParam(required = false) Long cashierId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        Authentication authentication) {
        return ApiResponse.success("Sales retrieved",
            retailService.listSales(fromDate, toDate, cashierId, page, size, principal(authentication)));
    }

    @GetMapping("/api/sales/{id}")
    public ApiResponse<SaleResponse> sale(@PathVariable @Min(1) long id, Authentication authentication) {
        return ApiResponse.success("Sale retrieved", retailService.getSale(id, principal(authentication)));
    }

    @PostMapping("/api/purchases")
    public ApiResponse<PurchaseResponse> createPurchase(
        @Valid @RequestBody PurchaseRequest request, Authentication authentication) {
        return ApiResponse.success("Purchase received", retailService.createPurchase(request, principal(authentication)));
    }

    @GetMapping("/api/purchases")
    public ApiResponse<PageResponse<PurchaseResponse>> purchases(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        Authentication authentication) {
        return ApiResponse.success("Purchases retrieved", retailService.listPurchases(page, size, principal(authentication)));
    }

    @PostMapping("/api/stock/adjust")
    public ApiResponse<StockMovementResponse> adjustStock(
        @Valid @RequestBody StockAdjustmentRequest request, Authentication authentication) {
        return ApiResponse.success("Stock adjusted", retailService.adjustStock(request, principal(authentication)));
    }

    @GetMapping("/api/stock/movements")
    public ApiResponse<PageResponse<StockMovementResponse>> stockMovements(
        @RequestParam(required = false) Long productId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        Authentication authentication) {
        return ApiResponse.success("Stock movements retrieved",
            retailService.listStockMovements(productId, page, size, principal(authentication)));
    }

    @GetMapping("/api/dashboard/retail")
    public ApiResponse<RetailDashboardResponse> dashboard(Authentication authentication) {
        return ApiResponse.success("Retail dashboard retrieved", retailService.getDashboard(principal(authentication)));
    }

    private AuthUserPrincipal principal(Authentication authentication) {
        return (AuthUserPrincipal) authentication.getPrincipal();
    }
}
