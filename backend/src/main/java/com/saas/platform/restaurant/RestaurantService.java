package com.saas.platform.restaurant;

import com.saas.platform.common.ConflictException;
import com.saas.platform.common.PageResponse;
import com.saas.platform.common.ResourceNotFoundException;
import com.saas.platform.core.Settings;
import com.saas.platform.core.SettingsRepository;
import com.saas.platform.core.User;
import com.saas.platform.core.UserRepository;
import com.saas.platform.security.AuthUserPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantService {

    private static final List<String> OPEN_ORDER_STATUSES = List.of("PLACED", "PREPARING", "READY", "SERVED");
    private static final List<String> ACTIVE_ORDER_STATUSES = List.of("PLACED", "PREPARING", "READY");
    private static final Set<String> ORDER_STATUSES = Set.of("PLACED", "PREPARING", "READY", "SERVED", "BILLED");
    private static final Set<String> PAYMENT_MODES = Set.of("CASH", "CARD", "UPI");

    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final DiningTableRepository diningTableRepository;
    private final RestaurantOrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BillRepository billRepository;
    private final UserRepository userRepository;
    private final SettingsRepository settingsRepository;

    public RestaurantService(
        CategoryRepository categoryRepository,
        MenuItemRepository menuItemRepository,
        DiningTableRepository diningTableRepository,
        RestaurantOrderRepository orderRepository,
        OrderItemRepository orderItemRepository,
        BillRepository billRepository,
        UserRepository userRepository,
        SettingsRepository settingsRepository) {
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.diningTableRepository = diningTableRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.billRepository = billRepository;
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public PageResponse<CategoryResponse> listCategories(AuthUserPrincipal actor, int page, int size) {
        requireRestaurant(actor);
        return PageResponse.from(categoryRepository.findAll(PageRequest.of(page, size)).map(CategoryResponse::from));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public CategoryResponse createCategory(CategoryRequest request, AuthUserPrincipal actor) {
        requireRestaurant(actor);
        requireOwnerOrManager(actor);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A category with this name already exists");
        }
        Category category = new Category();
        category.setName(name);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public CategoryResponse updateCategory(long id, CategoryRequest request, AuthUserPrincipal actor) {
        requireRestaurant(actor);
        requireOwnerOrManager(actor);
        Category category = findCategory(id);
        String name = request.name().trim();
        if (!name.equalsIgnoreCase(category.getName()) && categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A category with this name already exists");
        }
        category.setName(name);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteCategory(long id, AuthUserPrincipal actor) {
        requireRestaurant(actor);
        requireOwnerOrManager(actor);
        Category category = findCategory(id);
        if (menuItemRepository.existsByCategoryId(id)) {
            throw new ConflictException("Move or remove this category's menu items before deleting it");
        }
        categoryRepository.delete(category);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public PageResponse<MenuItemResponse> listMenuItems(
        AuthUserPrincipal actor,
        Long categoryId,
        String search,
        int page,
        int size) {
        requireRestaurant(actor);
        PageRequest pageable = PageRequest.of(page, size);
        String term = search == null ? "" : search.trim();
        Page<MenuItem> items;
        if (categoryId != null && !term.isEmpty()) {
            items = menuItemRepository.findByCategoryIdAndNameContainingIgnoreCase(categoryId, term, pageable);
        } else if (categoryId != null) {
            items = menuItemRepository.findByCategoryId(categoryId, pageable);
        } else if (!term.isEmpty()) {
            items = menuItemRepository.findByNameContainingIgnoreCase(term, pageable);
        } else {
            items = menuItemRepository.findAll(pageable);
        }
        return PageResponse.from(items.map(MenuItemResponse::from));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public MenuItemResponse createMenuItem(MenuItemRequest request, AuthUserPrincipal actor) {
        requireRestaurant(actor);
        requireOwnerOrManager(actor);
        MenuItem item = new MenuItem();
        applyMenuItem(item, request);
        item.setAvailable(true);
        return MenuItemResponse.from(menuItemRepository.save(item));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public MenuItemResponse updateMenuItem(long id, MenuItemRequest request, AuthUserPrincipal actor) {
        requireRestaurant(actor);
        requireOwnerOrManager(actor);
        MenuItem item = findMenuItem(id);
        applyMenuItem(item, request);
        return MenuItemResponse.from(menuItemRepository.save(item));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public MenuItemResponse setAvailability(long id, boolean available, AuthUserPrincipal actor) {
        requireRestaurant(actor);
        requireOwnerOrManager(actor);
        MenuItem item = findMenuItem(id);
        item.setAvailable(available);
        return MenuItemResponse.from(menuItemRepository.save(item));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteMenuItem(long id, AuthUserPrincipal actor) {
        requireRestaurant(actor);
        requireOwnerOrManager(actor);
        MenuItem item = findMenuItem(id);
        if (orderItemRepository.existsByMenuItemId(id)) {
            throw new ConflictException("Menu items referenced by existing orders cannot be deleted; mark the item unavailable instead");
        }
        menuItemRepository.delete(item);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<DiningTableResponse> listTables(AuthUserPrincipal actor) {
        requireRestaurant(actor);
        return diningTableRepository.findAll().stream().map(DiningTableResponse::from).toList();
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public DiningTableResponse createTable(DiningTableRequest request, AuthUserPrincipal actor) {
        requireRestaurant(actor);
        requireOwnerOrManager(actor);
        String name = request.name().trim();
        if (diningTableRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A table with this name already exists");
        }
        DiningTable table = new DiningTable();
        table.setName(name);
        table.setCapacity(request.capacity());
        table.setStatus("FREE");
        return DiningTableResponse.from(diningTableRepository.save(table));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public DiningTableResponse updateTable(long id, DiningTableRequest request, AuthUserPrincipal actor) {
        requireRestaurant(actor);
        requireOwnerOrManager(actor);
        DiningTable table = findTableForUpdate(id);
        if (!"FREE".equals(table.getStatus())) {
            throw new ConflictException("Only free tables can be edited");
        }
        String name = request.name().trim();
        if (!name.equalsIgnoreCase(table.getName()) && diningTableRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A table with this name already exists");
        }
        table.setName(name);
        table.setCapacity(request.capacity());
        return DiningTableResponse.from(diningTableRepository.save(table));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteTable(long id, AuthUserPrincipal actor) {
        requireRestaurant(actor);
        requireOwnerOrManager(actor);
        DiningTable table = findTableForUpdate(id);
        if (!"FREE".equals(table.getStatus())
            || orderRepository.existsByDiningTableIdAndStatusIn(id, OPEN_ORDER_STATUSES)) {
            throw new ConflictException("Only tables without an open order can be deleted");
        }
        diningTableRepository.delete(table);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public RestaurantOrderResponse createOrder(CreateOrderRequest request, AuthUserPrincipal actor) {
        requireRestaurant(actor);
        requireAnyRole(actor, "OWNER", "MANAGER", "WAITER");
        DiningTable table = findTableForUpdate(request.tableId());
        if (!"FREE".equals(table.getStatus())) {
            throw new ConflictException("This table already has an active order");
        }

        RestaurantOrder order = new RestaurantOrder();
        order.setDiningTable(table);
        order.setWaiter(findUser(actor.userId()));
        order.setStatus("PLACED");
        order = orderRepository.save(order);
        List<OrderItem> items = buildOrderItems(request.items(), order);
        orderItemRepository.saveAll(items);
        table.setStatus("OCCUPIED");
        diningTableRepository.save(table);
        return RestaurantOrderResponse.from(order, items);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public RestaurantOrderResponse addOrderItems(long id, AddOrderItemsRequest request, AuthUserPrincipal actor) {
        requireRestaurant(actor);
        requireAnyRole(actor, "OWNER", "MANAGER", "WAITER");
        RestaurantOrder order = findOrderForUpdate(id);
        if ("BILLED".equals(order.getStatus())) {
            throw new ConflictException("Items cannot be added to a billed order");
        }
        List<OrderItem> newItems = buildOrderItems(request.items(), order);
        orderItemRepository.saveAll(newItems);
        return RestaurantOrderResponse.from(order, orderItemRepository.findByOrderIdOrderByIdAsc(id));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public RestaurantOrderResponse changeOrderStatus(long id, String requestedStatus, AuthUserPrincipal actor) {
        requireRestaurant(actor);
        RestaurantOrder order = findOrderForUpdate(id);
        String nextStatus = normalizeOrderStatus(requestedStatus);
        validateTransition(order.getStatus(), nextStatus, actor.role());
        order.setStatus(nextStatus);
        return RestaurantOrderResponse.from(
            orderRepository.save(order),
            orderItemRepository.findByOrderIdOrderByIdAsc(id)
        );
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<RestaurantOrderResponse> listOrders(String requestedStatus, AuthUserPrincipal actor) {
        requireRestaurant(actor);
        List<RestaurantOrder> orders;
        if (requestedStatus != null && !requestedStatus.isBlank()) {
            String status = normalizeOrderStatus(requestedStatus);
            orders = orderRepository.findByStatusOrderByCreatedAtDesc(status);
        } else if (isKitchen(actor.role())) {
            orders = orderRepository.findByStatusInOrderByCreatedAtAsc(List.of("PLACED", "PREPARING"));
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc();
        }
        return toOrderResponses(orders);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public BillResponse billOrder(long orderId, String requestedPaymentMode, AuthUserPrincipal actor) {
        requireRestaurant(actor);
        requireAnyRole(actor, "OWNER", "MANAGER", "CASHIER");
        String paymentMode = normalizePaymentMode(requestedPaymentMode);
        RestaurantOrder order = findOrderForUpdate(orderId);
        if (!"SERVED".equals(order.getStatus())) {
            throw new ConflictException("Only served orders can be billed");
        }
        if (billRepository.existsByOrderId(orderId)) {
            throw new ConflictException("This order has already been billed");
        }
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByIdAsc(orderId);
        if (items.isEmpty()) {
            throw new ConflictException("An empty order cannot be billed");
        }
        BigDecimal subtotal = items.stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxRate = settingsRepository.findByKey("tax_percent")
            .map(Settings::getValue)
            .filter(value -> value != null && !value.isBlank())
            .map(BigDecimal::new)
            .orElseThrow(() -> new IllegalStateException("Business tax setting is not configured"));
        BigDecimal tax = subtotal.multiply(taxRate)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        Bill bill = new Bill();
        bill.setOrder(order);
        bill.setSubtotal(subtotal);
        bill.setTax(tax);
        bill.setTotal(subtotal.add(tax).setScale(2, RoundingMode.HALF_UP));
        bill.setPaymentMode(paymentMode);
        bill.setCashier(findUser(actor.userId()));
        Bill savedBill = billRepository.save(bill);

        order.setStatus("BILLED");
        orderRepository.save(order);
        DiningTable table = findTableForUpdate(order.getDiningTable().getId());
        table.setStatus("FREE");
        diningTableRepository.save(table);
        return BillResponse.from(savedBill, items);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public BillResponse getBill(long billId, AuthUserPrincipal actor) {
        requireRestaurant(actor);
        Bill bill = billRepository.findById(billId)
            .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
        return BillResponse.from(bill, orderItemRepository.findByOrderIdOrderByIdAsc(bill.getOrder().getId()));
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public RestaurantDashboardResponse getDashboard(AuthUserPrincipal actor) {
        requireRestaurant(actor);
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();
        LocalDateTime startOfWeek = today.minusDays(6).atStartOfDay();

        List<Bill> todaysBills = billRepository.findByPaidAtGreaterThanEqualAndPaidAtLessThan(startOfToday, startOfTomorrow);
        BigDecimal todaysSales = todaysBills.stream()
            .map(Bill::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
        List<RestaurantOrder> todaysOrders =
            orderRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startOfToday, startOfTomorrow);
        List<RestaurantOrder> recentOrders =
            orderRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startOfWeek, startOfTomorrow);
        List<Long> recentOrderIds = recentOrders.stream().map(RestaurantOrder::getId).toList();
        List<OrderItem> recentItems = recentOrderIds.isEmpty()
            ? List.of()
            : orderItemRepository.findByOrderIdIn(recentOrderIds);

        Map<String, Long> dishQuantities = new HashMap<>();
        recentItems.forEach(item -> dishQuantities.merge(item.getMenuItem().getName(), (long) item.getQuantity(), Long::sum));
        List<TopDishResponse> topDishes = dishQuantities.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
            .limit(5)
            .map(entry -> new TopDishResponse(entry.getKey(), entry.getValue()))
            .toList();

        long[] hourlyCounts = new long[24];
        todaysOrders.stream()
            .filter(order -> order.getCreatedAt() != null)
            .forEach(order -> hourlyCounts[order.getCreatedAt().getHour()]++);
        List<OrdersPerHourResponse> perHour = new ArrayList<>(24);
        for (int hour = 0; hour < hourlyCounts.length; hour++) {
            perHour.add(new OrdersPerHourResponse(hour, hourlyCounts[hour]));
        }

        return new RestaurantDashboardResponse(
            todaysSales,
            orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startOfToday, startOfTomorrow),
            orderRepository.countByStatusIn(ACTIVE_ORDER_STATUSES),
            topDishes,
            perHour
        );
    }

    private List<OrderItem> buildOrderItems(List<OrderLineRequest> lines, RestaurantOrder order) {
        List<Long> requestedIds = lines.stream().map(OrderLineRequest::menuItemId).distinct().toList();
        Map<Long, MenuItem> availableItems = new HashMap<>();
        menuItemRepository.findByIdIn(requestedIds).forEach(item -> availableItems.put(item.getId(), item));

        List<OrderItem> items = new ArrayList<>();
        for (OrderLineRequest line : lines) {
            MenuItem menuItem = availableItems.get(line.menuItemId());
            if (menuItem == null) {
                throw new ResourceNotFoundException("Menu item not found");
            }
            if (!menuItem.isAvailable()) {
                throw new ConflictException("Menu item is unavailable: " + menuItem.getName());
            }
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setMenuItem(menuItem);
            item.setQuantity(line.quantity());
            item.setUnitPrice(menuItem.getPrice());
            item.setNotes(line.notes() == null || line.notes().isBlank() ? null : line.notes().trim());
            items.add(item);
        }
        return items;
    }

    private List<RestaurantOrderResponse> toOrderResponses(List<RestaurantOrder> orders) {
        if (orders.isEmpty()) return List.of();
        List<Long> ids = orders.stream().map(RestaurantOrder::getId).toList();
        Map<Long, List<OrderItem>> itemsByOrder = new HashMap<>();
        orderItemRepository.findByOrderIdIn(ids).forEach(item ->
            itemsByOrder.computeIfAbsent(item.getOrder().getId(), ignored -> new ArrayList<>()).add(item));
        return orders.stream()
            .map(order -> RestaurantOrderResponse.from(order, itemsByOrder.getOrDefault(order.getId(), List.of())))
            .toList();
    }

    private void applyMenuItem(MenuItem item, MenuItemRequest request) {
        Category category = findCategory(request.categoryId());
        item.setCategory(category);
        item.setName(request.name().trim());
        item.setDescription(request.description() == null || request.description().isBlank() ? null : request.description().trim());
        item.setPrice(request.price().setScale(2, RoundingMode.HALF_UP));
    }

    private void validateTransition(String current, String next, String role) {
        boolean valid = switch (current) {
            case "PLACED" -> "PREPARING".equals(next) && (isKitchen(role) || isManagement(role));
            case "PREPARING" -> "READY".equals(next) && (isKitchen(role) || isManagement(role));
            case "READY" -> "SERVED".equals(next) && ("WAITER".equals(role) || isManagement(role));
            default -> false;
        };
        if (!valid) {
            throw new ConflictException("Invalid order status transition from " + current + " to " + next);
        }
    }

    private String normalizeOrderStatus(String requestedStatus) {
        if (requestedStatus == null || requestedStatus.isBlank()) {
            throw new IllegalArgumentException("Order status is required");
        }
        String normalized = requestedStatus.trim().toUpperCase(Locale.ROOT);
        if (!ORDER_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported order status: " + requestedStatus);
        }
        return normalized;
    }

    private String normalizePaymentMode(String requestedMode) {
        if (requestedMode == null || requestedMode.isBlank()) {
            throw new IllegalArgumentException("Payment mode is required");
        }
        String normalized = requestedMode.trim().toUpperCase(Locale.ROOT);
        if (!PAYMENT_MODES.contains(normalized)) {
            throw new IllegalArgumentException("Payment mode must be CASH, CARD, or UPI");
        }
        return normalized;
    }

    private void requireRestaurant(AuthUserPrincipal actor) {
        if (actor == null || !"RESTAURANT".equals(actor.businessType())) {
            throw new AccessDeniedException("Restaurant features are available only to restaurant businesses");
        }
    }

    private void requireOwnerOrManager(AuthUserPrincipal actor) {
        requireAnyRole(actor, "OWNER", "MANAGER");
    }

    private void requireAnyRole(AuthUserPrincipal actor, String... roles) {
        for (String role : roles) {
            if (role.equals(actor.role())) return;
        }
        throw new AccessDeniedException("Your role cannot perform this restaurant operation");
    }

    private boolean isKitchen(String role) {
        return "KITCHEN".equals(role) || "KITCHEN_STAFF".equals(role);
    }

    private boolean isManagement(String role) {
        return "OWNER".equals(role) || "MANAGER".equals(role);
    }

    private Category findCategory(long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private MenuItem findMenuItem(long id) {
        return menuItemRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
    }

    private DiningTable findTableForUpdate(long id) {
        return diningTableRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Dining table not found"));
    }

    private RestaurantOrder findOrderForUpdate(long id) {
        return orderRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private User findUser(long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
