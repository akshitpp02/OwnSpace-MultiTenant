package com.saas.platform.retail;

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

class RetailServiceTest {

    @Test
    void restaurantTokenIsRejectedBeforeRetailDataIsRead() {
        ProductRepository products = mock(ProductRepository.class);
        RetailService service = service(mock(RetailCategoryRepository.class), products);

        assertThrows(AccessDeniedException.class,
            () -> service.listProducts(null, null, null, 0, 20, principal("RESTAURANT", "OWNER")));

        verify(products, never()).search(any(), any(), any(), any());
    }

    @Test
    void cashierCannotChangeProductPriceOrCost() {
        ProductRepository products = mock(ProductRepository.class);
        Product product = product(3L, "Milk", "MILK-1", "4.00", "2.00", 5, true);
        when(products.findByIdForUpdate(3L)).thenReturn(Optional.of(product));
        RetailService service = service(mock(RetailCategoryRepository.class), products);

        assertThrows(AccessDeniedException.class, () -> service.updateProduct(
            3L, new ProductRequest(null, "Milk", "MILK-1", new BigDecimal("8.00"), new BigDecimal("6.00"), null, 5, true),
            principal("RETAIL", "CASHIER")));

        assertEquals(new BigDecimal("4.00"), product.getPrice());
        assertEquals(new BigDecimal("2.00"), product.getCostPrice());
    }

    @Test
    void saleSnapshotsPricesAndAtomicallyReducesInventory() {
        Product product = product(3L, "Milk", "MILK-1", "4.25", "2.00", 10, true);
        ProductRepository products = mock(ProductRepository.class);
        SaleRepository sales = mock(SaleRepository.class);
        SaleItemRepository saleItems = mock(SaleItemRepository.class);
        StockMovementRepository movements = mock(StockMovementRepository.class);
        UserRepository users = mock(UserRepository.class);
        SettingsRepository settings = mock(SettingsRepository.class);
        when(products.findByIdForUpdate(3L)).thenReturn(Optional.of(product));
        when(sales.save(any(Sale.class))).thenAnswer(invocation -> {
            Sale sale = invocation.getArgument(0);
            sale.setId(18L);
            return sale;
        });
        when(saleItems.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.findById(7L)).thenReturn(Optional.of(user(7L)));
        Settings tax = new Settings();
        tax.setValue("5");
        when(settings.findByKey("tax_percent")).thenReturn(Optional.of(tax));
        RetailService service = service(mock(RetailCategoryRepository.class), products, mock(CustomerRepository.class),
            mock(SupplierRepository.class), sales, saleItems, mock(PurchaseRepository.class),
            mock(PurchaseItemRepository.class), movements, users, settings);

        SaleResponse response = service.createSale(
            new SaleRequest(List.of(new SaleLineRequest(3L, 3)), null, new BigDecimal("1.00"), "cash"),
            principal("RETAIL", "CASHIER"));

        assertEquals(new BigDecimal("12.75"), response.subtotal());
        assertEquals(new BigDecimal("0.59"), response.tax());
        assertEquals(new BigDecimal("12.34"), response.total());
        assertEquals("CASH", response.paymentMode());
        assertEquals(new BigDecimal("4.25"), response.items().get(0).unitPrice());
        assertEquals(new BigDecimal("2.00"), response.items().get(0).unitCost());
        assertEquals(7, product.getStockQty());
        verify(movements).save(any(StockMovement.class));
    }

    @Test
    void rejectedSaleDoesNotPartiallyPersistOrReduceStock() {
        Product available = product(3L, "Milk", "MILK-1", "4.25", "2.00", 10, true);
        Product shortStock = product(4L, "Bread", "BREAD-1", "3.00", "1.00", 1, true);
        ProductRepository products = mock(ProductRepository.class);
        SaleRepository sales = mock(SaleRepository.class);
        when(products.findByIdForUpdate(3L)).thenReturn(Optional.of(available));
        when(products.findByIdForUpdate(4L)).thenReturn(Optional.of(shortStock));
        RetailService service = service(mock(RetailCategoryRepository.class), products, mock(CustomerRepository.class),
            mock(SupplierRepository.class), sales, mock(SaleItemRepository.class), mock(PurchaseRepository.class),
            mock(PurchaseItemRepository.class), mock(StockMovementRepository.class), mock(UserRepository.class),
            mock(SettingsRepository.class));

        assertThrows(ConflictException.class, () -> service.createSale(
            new SaleRequest(List.of(new SaleLineRequest(3L, 2), new SaleLineRequest(4L, 2)), null, null, "CARD"),
            principal("RETAIL", "CASHIER")));

        assertEquals(10, available.getStockQty());
        assertEquals(1, shortStock.getStockQty());
        verify(sales, never()).save(any(Sale.class));
    }

    @Test
    void purchaseReceivesStockAndUpdatesCostWithMovement() {
        Product product = product(3L, "Milk", "MILK-1", "4.25", "2.00", 10, true);
        Supplier supplier = new Supplier();
        supplier.setId(9L);
        supplier.setName("Dairy Co.");
        ProductRepository products = mock(ProductRepository.class);
        SupplierRepository suppliers = mock(SupplierRepository.class);
        PurchaseRepository purchases = mock(PurchaseRepository.class);
        PurchaseItemRepository purchaseItems = mock(PurchaseItemRepository.class);
        StockMovementRepository movements = mock(StockMovementRepository.class);
        UserRepository users = mock(UserRepository.class);
        when(suppliers.findById(9L)).thenReturn(Optional.of(supplier));
        when(products.findByIdForUpdate(3L)).thenReturn(Optional.of(product));
        when(purchases.save(any(Purchase.class))).thenAnswer(invocation -> {
            Purchase purchase = invocation.getArgument(0);
            purchase.setId(26L);
            return purchase;
        });
        when(purchaseItems.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(users.findById(7L)).thenReturn(Optional.of(user(7L)));
        RetailService service = service(mock(RetailCategoryRepository.class), products, mock(CustomerRepository.class),
            suppliers, mock(SaleRepository.class), mock(SaleItemRepository.class), purchases, purchaseItems,
            movements, users, mock(SettingsRepository.class));

        PurchaseResponse response = service.createPurchase(
            new PurchaseRequest(9L, List.of(new PurchaseLineRequest(3L, 50, new BigDecimal("2.50")))),
            principal("RETAIL", "STOCK_KEEPER"));

        assertEquals(new BigDecimal("125.00"), response.totalCost());
        assertEquals(60, product.getStockQty());
        assertEquals(new BigDecimal("2.50"), product.getCostPrice());
        verify(movements).save(any(StockMovement.class));
    }

    @Test
    void stockAdjustmentCannotMakeInventoryNegative() {
        Product product = product(3L, "Milk", "MILK-1", "4.25", "2.00", 1, true);
        ProductRepository products = mock(ProductRepository.class);
        when(products.findByIdForUpdate(3L)).thenReturn(Optional.of(product));
        RetailService service = service(mock(RetailCategoryRepository.class), products);

        assertThrows(ConflictException.class, () -> service.adjustStock(
            new StockAdjustmentRequest(3L, -2, "Damaged"), principal("RETAIL", "STOCK_KEEPER")));
        assertEquals(1, product.getStockQty());
    }

    private RetailService service(RetailCategoryRepository categories, ProductRepository products) {
        return service(categories, products, mock(CustomerRepository.class), mock(SupplierRepository.class),
            mock(SaleRepository.class), mock(SaleItemRepository.class), mock(PurchaseRepository.class),
            mock(PurchaseItemRepository.class), mock(StockMovementRepository.class), mock(UserRepository.class),
            mock(SettingsRepository.class));
    }

    private RetailService service(
        RetailCategoryRepository categories, ProductRepository products, CustomerRepository customers,
        SupplierRepository suppliers, SaleRepository sales, SaleItemRepository saleItems,
        PurchaseRepository purchases, PurchaseItemRepository purchaseItems, StockMovementRepository movements,
        UserRepository users, SettingsRepository settings) {
        return new RetailService(categories, products, customers, suppliers, sales, saleItems, purchases,
            purchaseItems, movements, users, settings);
    }

    private AuthUserPrincipal principal(String businessType, String role) {
        return new AuthUserPrincipal(7L, 1L, "retail@example.com", role, businessType);
    }

    private Product product(long id, String name, String sku, String price, String cost, int stock, boolean active) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setSku(sku);
        product.setPrice(new BigDecimal(price));
        product.setCostPrice(new BigDecimal(cost));
        product.setStockQty(stock);
        product.setActive(active);
        product.setLowStockThreshold(5);
        return product;
    }

    private User user(long id) {
        User user = new User();
        user.setId(id);
        user.setName("Retail User");
        return user;
    }
}
