package com.saas.platform.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saas.platform.common.ConflictException;
import com.saas.platform.core.Settings;
import com.saas.platform.core.SettingsRepository;
import com.saas.platform.core.User;
import com.saas.platform.core.UserRepository;
import com.saas.platform.security.AuthUserPrincipal;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class RestaurantServiceTest {

    @Test
    void retailUsersAreDeniedFromRestaurantOperations() {
        CategoryRepository categories = mock(CategoryRepository.class);
        RestaurantService service = service(categories, mock(MenuItemRepository.class));

        assertThrows(AccessDeniedException.class, () -> service.listCategories(principal("RETAIL", "OWNER"), 0, 10));

        verify(categories, never()).findAll(any(org.springframework.data.domain.Pageable.class));
    }

    @Test
    void waitersCannotManageMenuItems() {
        MenuItemRepository menuItems = mock(MenuItemRepository.class);
        RestaurantService service = service(mock(CategoryRepository.class), menuItems);

        assertThrows(
            AccessDeniedException.class,
            () -> service.createMenuItem(
                new MenuItemRequest(1L, "Soup", null, new BigDecimal("8.50")),
                principal("RESTAURANT", "WAITER")
            )
        );

        verify(menuItems, never()).save(any(MenuItem.class));
    }

    @Test
    void cannotCreateAnOrderWithAnUnavailableMenuItem() {
        DiningTable table = table(4L, "Table 4", "FREE");
        MenuItem unavailable = menuItem(7L, "Soup", "8.50", false);
        DiningTableRepository tables = mock(DiningTableRepository.class);
        RestaurantOrderRepository orders = mock(RestaurantOrderRepository.class);
        MenuItemRepository menuItems = mock(MenuItemRepository.class);
        UserRepository users = mock(UserRepository.class);
        when(tables.findByIdForUpdate(4L)).thenReturn(Optional.of(table));
        when(orders.save(any(RestaurantOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(menuItems.findByIdIn(List.of(7L))).thenReturn(List.of(unavailable));
        when(users.findById(12L)).thenReturn(Optional.of(user(12L, "Pat")));

        RestaurantService service = service(mock(CategoryRepository.class), menuItems, tables, orders,
            mock(OrderItemRepository.class), mock(BillRepository.class), users, mock(SettingsRepository.class));

        assertThrows(
            ConflictException.class,
            () -> service.createOrder(new CreateOrderRequest(4L, List.of(new OrderLineRequest(7L, 1, null))),
                principal("RESTAURANT", "WAITER"))
        );
        verify(tables, never()).save(any(DiningTable.class));
    }

    @Test
    void orderCreationSnapshotsPriceAndOccupiesTable() {
        DiningTable table = table(4L, "Table 4", "FREE");
        User waiter = user(12L, "Pat");
        MenuItem dish = menuItem(7L, "Soup", "8.50", true);
        DiningTableRepository tables = mock(DiningTableRepository.class);
        RestaurantOrderRepository orders = mock(RestaurantOrderRepository.class);
        MenuItemRepository menuItems = mock(MenuItemRepository.class);
        UserRepository users = mock(UserRepository.class);
        when(tables.findByIdForUpdate(4L)).thenReturn(Optional.of(table));
        when(orders.save(any(RestaurantOrder.class))).thenAnswer(invocation -> {
            RestaurantOrder order = invocation.getArgument(0);
            order.setId(25L);
            return order;
        });
        when(menuItems.findByIdIn(List.of(7L))).thenReturn(List.of(dish));
        when(users.findById(12L)).thenReturn(Optional.of(waiter));
        RestaurantService service = service(mock(CategoryRepository.class), menuItems, tables, orders,
            mock(OrderItemRepository.class), mock(BillRepository.class), users, mock(SettingsRepository.class));

        RestaurantOrderResponse created = service.createOrder(
            new CreateOrderRequest(4L, List.of(new OrderLineRequest(7L, 2, "No onions"))),
            principal("RESTAURANT", "WAITER")
        );

        assertEquals(25L, created.id());
        assertEquals("OCCUPIED", table.getStatus());
        assertEquals(new BigDecimal("8.50"), created.items().get(0).unitPrice());
        assertEquals(new BigDecimal("17.00"), created.items().get(0).lineTotal());
    }

    @Test
    void billingRoundsTaxAndFreesTable() {
        DiningTable table = table(4L, "Table 4", "OCCUPIED");
        RestaurantOrder order = order(25L, table, "SERVED");
        User cashier = user(15L, "Casey");
        MenuItem dish = menuItem(7L, "Soup", "8.35", true);
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setMenuItem(dish);
        item.setQuantity(3);
        item.setUnitPrice(new BigDecimal("8.35"));
        Settings taxSetting = new Settings();
        taxSetting.setValue("5");

        RestaurantOrderRepository orders = mock(RestaurantOrderRepository.class);
        DiningTableRepository tables = mock(DiningTableRepository.class);
        OrderItemRepository orderItems = mock(OrderItemRepository.class);
        BillRepository bills = mock(BillRepository.class);
        UserRepository users = mock(UserRepository.class);
        SettingsRepository settings = mock(SettingsRepository.class);
        when(orders.findByIdForUpdate(25L)).thenReturn(Optional.of(order));
        when(orderItems.findByOrderIdOrderByIdAsc(25L)).thenReturn(List.of(item));
        when(bills.existsByOrderId(25L)).thenReturn(false);
        when(bills.save(any(Bill.class))).thenAnswer(invocation -> {
            Bill bill = invocation.getArgument(0);
            bill.setId(31L);
            return bill;
        });
        when(tables.findByIdForUpdate(4L)).thenReturn(Optional.of(table));
        when(users.findById(15L)).thenReturn(Optional.of(cashier));
        when(settings.findByKey("tax_percent")).thenReturn(Optional.of(taxSetting));
        RestaurantService service = service(mock(CategoryRepository.class), mock(MenuItemRepository.class), tables,
            orders, orderItems, bills, users, settings);

        BillResponse bill = service.billOrder(25L, "cash", principal(15L, "RESTAURANT", "CASHIER"));

        assertEquals(new BigDecimal("25.05"), bill.subtotal());
        assertEquals(new BigDecimal("1.25"), bill.tax());
        assertEquals(new BigDecimal("26.30"), bill.total());
        assertEquals("CASH", bill.paymentMode());
        assertEquals("BILLED", order.getStatus());
        assertEquals("FREE", table.getStatus());
    }

    @Test
    void duplicateBillingIsRejected() {
        RestaurantOrder order = order(25L, table(4L, "Table 4", "OCCUPIED"), "SERVED");
        RestaurantOrderRepository orders = mock(RestaurantOrderRepository.class);
        BillRepository bills = mock(BillRepository.class);
        when(orders.findByIdForUpdate(25L)).thenReturn(Optional.of(order));
        when(bills.existsByOrderId(25L)).thenReturn(true);
        RestaurantService service = service(mock(CategoryRepository.class), mock(MenuItemRepository.class),
            mock(DiningTableRepository.class), orders, mock(OrderItemRepository.class), bills,
            mock(UserRepository.class), mock(SettingsRepository.class));

        assertThrows(
            ConflictException.class,
            () -> service.billOrder(25L, "CARD", principal("RESTAURANT", "CASHIER"))
        );
        verify(bills, never()).save(any(Bill.class));
    }

    @Test
    void statusFlowRestrictsKitchenAndWaiterActions() {
        RestaurantOrder order = order(25L, table(4L, "Table 4", "OCCUPIED"), "PLACED");
        RestaurantOrderRepository orders = mock(RestaurantOrderRepository.class);
        when(orders.findByIdForUpdate(25L)).thenReturn(Optional.of(order));
        when(orders.save(any(RestaurantOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        RestaurantService service = service(mock(CategoryRepository.class), mock(MenuItemRepository.class),
            mock(DiningTableRepository.class), orders, mock(OrderItemRepository.class), mock(BillRepository.class),
            mock(UserRepository.class), mock(SettingsRepository.class));

        assertThrows(ConflictException.class,
            () -> service.changeOrderStatus(25L, "PREPARING", principal("RESTAURANT", "WAITER")));
        assertEquals("PLACED", order.getStatus());
        assertEquals("PREPARING",
            service.changeOrderStatus(25L, "PREPARING", principal("RESTAURANT", "KITCHEN")).status());
        assertEquals("READY",
            service.changeOrderStatus(25L, "READY", principal("RESTAURANT", "KITCHEN")).status());
        assertThrows(ConflictException.class,
            () -> service.changeOrderStatus(25L, "SERVED", principal("RESTAURANT", "KITCHEN")));
        assertEquals("SERVED",
            service.changeOrderStatus(25L, "SERVED", principal("RESTAURANT", "WAITER")).status());
    }

    private RestaurantService service(CategoryRepository categories, MenuItemRepository menuItems) {
        return service(categories, menuItems, mock(DiningTableRepository.class), mock(RestaurantOrderRepository.class));
    }

    private RestaurantService service(
        CategoryRepository categories,
        MenuItemRepository menuItems,
        DiningTableRepository tables,
        RestaurantOrderRepository orders) {
        return service(categories, menuItems, tables, orders, mock(OrderItemRepository.class),
            mock(BillRepository.class), mock(UserRepository.class), mock(SettingsRepository.class));
    }

    private RestaurantService service(
        CategoryRepository categories,
        MenuItemRepository menuItems,
        DiningTableRepository tables,
        RestaurantOrderRepository orders,
        OrderItemRepository orderItems,
        BillRepository bills,
        UserRepository users,
        SettingsRepository settings) {
        return new RestaurantService(categories, menuItems, tables, orders, orderItems, bills, users, settings);
    }

    private AuthUserPrincipal principal(String businessType, String role) {
        return principal(12L, businessType, role);
    }

    private AuthUserPrincipal principal(long userId, String businessType, String role) {
        return new AuthUserPrincipal(userId, 1L, "staff@example.com", role, businessType);
    }

    private DiningTable table(long id, String name, String status) {
        DiningTable table = new DiningTable();
        table.setId(id);
        table.setName(name);
        table.setStatus(status);
        table.setCapacity(4);
        return table;
    }

    private RestaurantOrder order(long id, DiningTable table, String status) {
        RestaurantOrder order = new RestaurantOrder();
        order.setId(id);
        order.setDiningTable(table);
        order.setWaiter(user(12L, "Pat"));
        order.setStatus(status);
        return order;
    }

    private User user(long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }

    private MenuItem menuItem(long id, String name, String price, boolean available) {
        MenuItem item = new MenuItem();
        item.setId(id);
        item.setName(name);
        item.setPrice(new BigDecimal(price));
        item.setAvailable(available);
        return item;
    }
}
