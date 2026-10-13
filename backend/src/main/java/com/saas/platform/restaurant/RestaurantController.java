package com.saas.platform.restaurant;

import com.saas.platform.common.ApiResponse;
import com.saas.platform.common.PageResponse;
import com.saas.platform.security.AuthUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    @GetMapping("/api/categories")
    public ApiResponse<PageResponse<CategoryResponse>> categories(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "100") @Min(1) @Max(100) int size,
        Authentication authentication) {
        return ApiResponse.success(
            "Categories retrieved",
            restaurantService.listCategories(principal(authentication), page, size)
        );
    }

    @PostMapping("/api/categories")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ApiResponse<CategoryResponse> createCategory(
        @Valid @RequestBody CategoryRequest request,
        Authentication authentication) {
        return ApiResponse.success("Category created", restaurantService.createCategory(request, principal(authentication)));
    }

    @PutMapping("/api/categories/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ApiResponse<CategoryResponse> updateCategory(
        @PathVariable @Min(1) long id,
        @Valid @RequestBody CategoryRequest request,
        Authentication authentication) {
        return ApiResponse.success("Category updated", restaurantService.updateCategory(id, request, principal(authentication)));
    }

    @DeleteMapping("/api/categories/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ApiResponse<Void> deleteCategory(@PathVariable @Min(1) long id, Authentication authentication) {
        restaurantService.deleteCategory(id, principal(authentication));
        return ApiResponse.success("Category deleted", null);
    }

    @GetMapping("/api/menu-items")
    public ApiResponse<PageResponse<MenuItemResponse>> menuItems(
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String search,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "100") @Min(1) @Max(100) int size,
        Authentication authentication) {
        return ApiResponse.success(
            "Menu items retrieved",
            restaurantService.listMenuItems(principal(authentication), categoryId, search, page, size)
        );
    }

    @PostMapping("/api/menu-items")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ApiResponse<MenuItemResponse> createMenuItem(
        @Valid @RequestBody MenuItemRequest request,
        Authentication authentication) {
        return ApiResponse.success("Menu item created", restaurantService.createMenuItem(request, principal(authentication)));
    }

    @PutMapping("/api/menu-items/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ApiResponse<MenuItemResponse> updateMenuItem(
        @PathVariable @Min(1) long id,
        @Valid @RequestBody MenuItemRequest request,
        Authentication authentication) {
        return ApiResponse.success("Menu item updated", restaurantService.updateMenuItem(id, request, principal(authentication)));
    }

    @PatchMapping("/api/menu-items/{id}/availability")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ApiResponse<MenuItemResponse> setAvailability(
        @PathVariable @Min(1) long id,
        @Valid @RequestBody AvailabilityRequest request,
        Authentication authentication) {
        return ApiResponse.success(
            "Menu item availability updated",
            restaurantService.setAvailability(id, request.available(), principal(authentication))
        );
    }

    @DeleteMapping("/api/menu-items/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ApiResponse<Void> deleteMenuItem(@PathVariable @Min(1) long id, Authentication authentication) {
        restaurantService.deleteMenuItem(id, principal(authentication));
        return ApiResponse.success("Menu item deleted", null);
    }

    @GetMapping("/api/tables")
    public ApiResponse<List<DiningTableResponse>> tables(Authentication authentication) {
        return ApiResponse.success("Dining tables retrieved", restaurantService.listTables(principal(authentication)));
    }

    @PostMapping("/api/tables")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ApiResponse<DiningTableResponse> createTable(
        @Valid @RequestBody DiningTableRequest request,
        Authentication authentication) {
        return ApiResponse.success("Dining table created", restaurantService.createTable(request, principal(authentication)));
    }

    @PutMapping("/api/tables/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ApiResponse<DiningTableResponse> updateTable(
        @PathVariable @Min(1) long id,
        @Valid @RequestBody DiningTableRequest request,
        Authentication authentication) {
        return ApiResponse.success("Dining table updated", restaurantService.updateTable(id, request, principal(authentication)));
    }

    @DeleteMapping("/api/tables/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER')")
    public ApiResponse<Void> deleteTable(@PathVariable @Min(1) long id, Authentication authentication) {
        restaurantService.deleteTable(id, principal(authentication));
        return ApiResponse.success("Dining table deleted", null);
    }

    @GetMapping("/api/orders")
    public ApiResponse<List<RestaurantOrderResponse>> orders(
        @RequestParam(required = false) String status,
        Authentication authentication) {
        return ApiResponse.success("Orders retrieved", restaurantService.listOrders(status, principal(authentication)));
    }

    @PostMapping("/api/orders")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'WAITER')")
    public ApiResponse<RestaurantOrderResponse> createOrder(
        @Valid @RequestBody CreateOrderRequest request,
        Authentication authentication) {
        return ApiResponse.success("Order created", restaurantService.createOrder(request, principal(authentication)));
    }

    @PostMapping("/api/orders/{id}/items")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'WAITER')")
    public ApiResponse<RestaurantOrderResponse> addItems(
        @PathVariable @Min(1) long id,
        @Valid @RequestBody AddOrderItemsRequest request,
        Authentication authentication) {
        return ApiResponse.success("Order items added", restaurantService.addOrderItems(id, request, principal(authentication)));
    }

    @PatchMapping("/api/orders/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'WAITER', 'KITCHEN', 'KITCHEN_STAFF')")
    public ApiResponse<RestaurantOrderResponse> changeOrderStatus(
        @PathVariable @Min(1) long id,
        @Valid @RequestBody OrderStatusRequest request,
        Authentication authentication) {
        return ApiResponse.success(
            "Order status updated",
            restaurantService.changeOrderStatus(id, request.status(), principal(authentication))
        );
    }

    @PostMapping("/api/orders/{id}/bill")
    @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'CASHIER')")
    public ApiResponse<BillResponse> billOrder(
        @PathVariable @Min(1) long id,
        @Valid @RequestBody PaymentRequest request,
        Authentication authentication) {
        return ApiResponse.success(
            "Order billed",
            restaurantService.billOrder(id, request.paymentMode(), principal(authentication))
        );
    }

    @GetMapping("/api/bills/{id}")
    public ApiResponse<BillResponse> bill(@PathVariable @Min(1) long id, Authentication authentication) {
        return ApiResponse.success("Bill retrieved", restaurantService.getBill(id, principal(authentication)));
    }

    @GetMapping("/api/dashboard/restaurant")
    public ApiResponse<RestaurantDashboardResponse> dashboard(Authentication authentication) {
        return ApiResponse.success("Restaurant dashboard retrieved", restaurantService.getDashboard(principal(authentication)));
    }

    private AuthUserPrincipal principal(Authentication authentication) {
        return (AuthUserPrincipal) authentication.getPrincipal();
    }
}
