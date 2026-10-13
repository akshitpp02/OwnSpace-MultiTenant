package com.saas.platform.retail;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RetailService {

    private static final Set<String> MANAGEMENT = Set.of("OWNER", "MANAGER");
    private static final Set<String> PRODUCT_MANAGERS = Set.of("OWNER", "MANAGER", "STOCK_KEEPER");
    private static final Set<String> PURCHASE_ROLES = Set.of("OWNER", "MANAGER", "STOCK_KEEPER");
    private static final Set<String> SALE_ROLES = Set.of("OWNER", "MANAGER", "CASHIER");
    private static final Set<String> PAYMENT_MODES = Set.of("CASH", "CARD", "UPI");

    private final RetailCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PurchaseRepository purchaseRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final SettingsRepository settingsRepository;

    public RetailService(
        RetailCategoryRepository categoryRepository,
        ProductRepository productRepository,
        CustomerRepository customerRepository,
        SupplierRepository supplierRepository,
        SaleRepository saleRepository,
        SaleItemRepository saleItemRepository,
        PurchaseRepository purchaseRepository,
        PurchaseItemRepository purchaseItemRepository,
        StockMovementRepository stockMovementRepository,
        UserRepository userRepository,
        SettingsRepository settingsRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.supplierRepository = supplierRepository;
        this.saleRepository = saleRepository;
        this.saleItemRepository = saleItemRepository;
        this.purchaseRepository = purchaseRepository;
        this.purchaseItemRepository = purchaseItemRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public PageResponse<RetailCategoryResponse> listCategories(AuthUserPrincipal actor, int page, int size) {
        requireRetail(actor);
        return PageResponse.from(categoryRepository.findAll(PageRequest.of(page, size, Sort.by("name"))).map(RetailCategoryResponse::from));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public RetailCategoryResponse createCategory(RetailCategoryRequest request, AuthUserPrincipal actor) {
        requireRetail(actor);
        requireAny(actor, "OWNER", "MANAGER", "STOCK_KEEPER");
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A category with this name already exists");
        }
        Category category = new Category();
        category.setName(name);
        return RetailCategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public RetailCategoryResponse updateCategory(long id, RetailCategoryRequest request, AuthUserPrincipal actor) {
        requireRetail(actor);
        requireAny(actor, "OWNER", "MANAGER", "STOCK_KEEPER");
        Category category = findCategory(id);
        String name = request.name().trim();
        if (!category.getName().equalsIgnoreCase(name) && categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A category with this name already exists");
        }
        category.setName(name);
        return RetailCategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteCategory(long id, AuthUserPrincipal actor) {
        requireRetail(actor);
        requireAny(actor, "OWNER", "MANAGER", "STOCK_KEEPER");
        Category category = findCategory(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("Move or remove this category's products before deleting it");
        }
        categoryRepository.delete(category);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public PageResponse<ProductResponse> listProducts(
        String search, Long categoryId, Boolean active, int page, int size, AuthUserPrincipal actor) {
        requireRetail(actor);
        Page<Product> products = productRepository.search(normalizeOptional(search), categoryId, active,
            PageRequest.of(page, size, Sort.by("name").ascending()));
        return PageResponse.from(products.map(ProductResponse::from));
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public List<ProductResponse> lowStockProducts(AuthUserPrincipal actor) {
        requireRetail(actor);
        return productRepository.findLowStock().stream().map(ProductResponse::from).toList();
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public ProductResponse createProduct(ProductRequest request, AuthUserPrincipal actor) {
        requireRetail(actor);
        requireAny(actor, "OWNER", "MANAGER", "STOCK_KEEPER");
        String sku = normalizeSku(request.sku());
        if (productRepository.existsBySkuIgnoreCase(sku)) {
            throw new ConflictException("A product with this SKU already exists");
        }
        Product product = new Product();
        applyProductDetails(product, request);
        product.setSku(sku);
        product.setPrice(canManagePrices(actor) ? moneyOrZero(request.price()) : BigDecimal.ZERO.setScale(2));
        product.setCostPrice(canManagePrices(actor) ? moneyOrZero(request.costPrice()) : BigDecimal.ZERO.setScale(2));
        product.setStockQty(request.stockQty() == null ? 0 : request.stockQty());
        product.setLowStockThreshold(request.lowStockThreshold() == null ? 5 : request.lowStockThreshold());
        product.setActive(request.active() == null || request.active());
        Product saved = productRepository.save(product);
        if (saved.getStockQty() > 0) {
            writeMovement(saved, saved.getStockQty(), "ADJUSTMENT", null, "Initial stock", actor);
        }
        return ProductResponse.from(saved);
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public ProductResponse updateProduct(long id, ProductRequest request, AuthUserPrincipal actor) {
        requireRetail(actor);
        requireAny(actor, "OWNER", "MANAGER", "STOCK_KEEPER");
        Product product = findProductForUpdate(id);
        String sku = normalizeSku(request.sku());
        if (!sku.equalsIgnoreCase(product.getSku()) && productRepository.existsBySkuIgnoreCase(sku)) {
            throw new ConflictException("A product with this SKU already exists");
        }
        applyProductDetails(product, request);
        product.setSku(sku);
        if (canManagePrices(actor)) {
            if (request.price() == null || request.costPrice() == null) {
                throw new IllegalArgumentException("Price and cost price are required for a manager update");
            }
            product.setPrice(money(request.price()));
            product.setCostPrice(money(request.costPrice()));
        }
        if (request.lowStockThreshold() != null) product.setLowStockThreshold(request.lowStockThreshold());
        if (request.active() != null) product.setActive(request.active());
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public ProductResponse setProductActive(long id, boolean active, AuthUserPrincipal actor) {
        requireRetail(actor);
        requireAny(actor, "OWNER", "MANAGER", "STOCK_KEEPER");
        Product product = findProductForUpdate(id);
        product.setActive(active);
        return ProductResponse.from(productRepository.save(product));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteProduct(long id, AuthUserPrincipal actor) {
        requireRetail(actor);
        requireAny(actor, "OWNER", "MANAGER", "STOCK_KEEPER");
        Product product = findProductForUpdate(id);
        if (product.getStockQty() != 0) {
            throw new ConflictException("Products with stock cannot be deleted; mark the product inactive instead");
        }
        productRepository.delete(product);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public PageResponse<CustomerResponse> listCustomers(String search, int page, int size, AuthUserPrincipal actor) {
        requireRetail(actor);
        String term = normalizeOptional(search);
        Page<Customer> customers = term == null
            ? customerRepository.findAll(PageRequest.of(page, size, Sort.by("name")))
            : customerRepository.findByNameContainingIgnoreCaseOrPhoneContainingIgnoreCase(
                term, term, PageRequest.of(page, size, Sort.by("name")));
        return PageResponse.from(customers.map(CustomerResponse::from));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public CustomerResponse createCustomer(CustomerRequest request, AuthUserPrincipal actor) {
        requireRetail(actor);
        requireAny(actor, "OWNER", "MANAGER", "CASHIER");
        String phone = request.phone().trim();
        if (customerRepository.existsByPhone(phone)) throw new ConflictException("A customer with this phone already exists");
        Customer customer = new Customer();
        applyCustomer(customer, request);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public CustomerResponse updateCustomer(long id, CustomerRequest request, AuthUserPrincipal actor) {
        requireRetail(actor);
        requireAny(actor, "OWNER", "MANAGER", "CASHIER");
        Customer customer = customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
        String phone = request.phone().trim();
        if (!phone.equals(customer.getPhone()) && customerRepository.existsByPhoneAndIdNot(phone, id)) {
            throw new ConflictException("A customer with this phone already exists");
        }
        applyCustomer(customer, request);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteCustomer(long id, AuthUserPrincipal actor) {
        requireRetail(actor);
        requireAny(actor, "OWNER", "MANAGER", "CASHIER");
        customerRepository.delete(customerRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found")));
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public PageResponse<SupplierResponse> listSuppliers(String search, int page, int size, AuthUserPrincipal actor) {
        requireRetail(actor);
        String term = normalizeOptional(search);
        Page<Supplier> suppliers = term == null
            ? supplierRepository.findAll(PageRequest.of(page, size, Sort.by("name")))
            : supplierRepository.findByNameContainingIgnoreCase(term, PageRequest.of(page, size, Sort.by("name")));
        return PageResponse.from(suppliers.map(SupplierResponse::from));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public SupplierResponse createSupplier(SupplierRequest request, AuthUserPrincipal actor) {
        requireRetail(actor);
        requireAny(actor, "OWNER", "MANAGER", "STOCK_KEEPER");
        Supplier supplier = new Supplier();
        applySupplier(supplier, request);
        return SupplierResponse.from(supplierRepository.save(supplier));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public SupplierResponse updateSupplier(long id, SupplierRequest request, AuthUserPrincipal actor) {
        requireRetail(actor);
        requireAny(actor, "OWNER", "MANAGER", "STOCK_KEEPER");
        Supplier supplier = supplierRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        applySupplier(supplier, request);
        return SupplierResponse.from(supplierRepository.save(supplier));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public void deleteSupplier(long id, AuthUserPrincipal actor) {
        requireRetail(actor);
        requireAny(actor, "OWNER", "MANAGER", "STOCK_KEEPER");
        supplierRepository.delete(supplierRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found")));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public SaleResponse createSale(SaleRequest request, AuthUserPrincipal actor) {
        requireRetail(actor);
        requireAny(actor, SALE_ROLES);
        String paymentMode = request.paymentMode().trim().toUpperCase(Locale.ROOT);
        if (!PAYMENT_MODES.contains(paymentMode)) throw new IllegalArgumentException("Payment mode must be CASH, CARD, or UPI");

        Map<Long, Integer> quantities = aggregateSaleQuantities(request.items());
        Map<Long, Product> lockedProducts = lockProducts(quantities.keySet());
        BigDecimal subtotal = BigDecimal.ZERO;
        List<SaleItem> saleItems = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = lockedProducts.get(entry.getKey());
            if (!product.isActive()) throw new ConflictException("Product is inactive: " + product.getName());
            if (product.getStockQty() < entry.getValue()) {
                throw new ConflictException("Insufficient stock for " + product.getName() + "; available: " + product.getStockQty());
            }
            SaleItem saleItem = new SaleItem();
            saleItem.setProduct(product);
            saleItem.setQuantity(entry.getValue());
            saleItem.setUnitPrice(product.getPrice());
            saleItem.setUnitCost(product.getCostPrice());
            saleItems.add(saleItem);
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(entry.getValue())));
        }
        subtotal = money(subtotal);
        BigDecimal discount = request.discount() == null ? BigDecimal.ZERO.setScale(2) : money(request.discount());
        if (discount.compareTo(subtotal) > 0) throw new IllegalArgumentException("Discount cannot exceed the subtotal");
        BigDecimal taxableAmount = subtotal.subtract(discount);
        BigDecimal tax = taxableAmount.multiply(taxRate())
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        Sale sale = new Sale();
        sale.setCashier(findUser(actor.userId()));
        sale.setCustomer(request.customerId() == null ? null : findCustomer(request.customerId()));
        sale.setSubtotal(subtotal);
        sale.setDiscount(discount);
        sale.setTax(tax);
        sale.setTotal(money(taxableAmount.add(tax)));
        sale.setPaymentMode(paymentMode);
        sale = saleRepository.save(sale);
        for (SaleItem item : saleItems) item.setSale(sale);
        saleItems = saleItemRepository.saveAll(saleItems);

        for (SaleItem item : saleItems) {
            Product product = item.getProduct();
            product.setStockQty(product.getStockQty() - item.getQuantity());
            writeMovement(product, -item.getQuantity(), "SALE", sale.getId(), null, actor);
        }
        return SaleResponse.from(sale, saleItems);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public PageResponse<SaleResponse> listSales(
        LocalDate fromDate, LocalDate toDate, Long cashierId, int page, int size, AuthUserPrincipal actor) {
        requireRetail(actor);
        validateDateRange(fromDate, toDate);
        LocalDateTime start = fromDate == null ? null : fromDate.atStartOfDay();
        LocalDateTime end = toDate == null ? null : toDate.plusDays(1).atStartOfDay();
        Page<Sale> sales = saleRepository.findFiltered(start, end, cashierId,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<Long> ids = sales.getContent().stream().map(Sale::getId).toList();
        Map<Long, List<SaleItem>> items = saleItemsBySale(ids);
        return PageResponse.from(sales.map(sale -> SaleResponse.from(sale, items.getOrDefault(sale.getId(), List.of()))));
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public SaleResponse getSale(long id, AuthUserPrincipal actor) {
        requireRetail(actor);
        Sale sale = saleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Sale not found"));
        return SaleResponse.from(sale, saleItemRepository.findBySaleIdOrderByIdAsc(id));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public PurchaseResponse createPurchase(PurchaseRequest request, AuthUserPrincipal actor) {
        requireRetail(actor);
        requireAny(actor, PURCHASE_ROLES);
        Supplier supplier = supplierRepository.findById(request.supplierId())
            .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));
        Map<Long, Integer> quantities = aggregatePurchaseQuantities(request.items());
        Map<Long, Product> lockedProducts = lockProducts(quantities.keySet());

        Map<Long, BigDecimal> unitCosts = new HashMap<>();
        for (PurchaseLineRequest line : request.items()) {
            unitCosts.put(line.productId(), money(line.unitCost()));
        }
        BigDecimal totalCost = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            totalCost = totalCost.add(unitCosts.get(entry.getKey()).multiply(BigDecimal.valueOf(entry.getValue())));
        }

        Purchase purchase = new Purchase();
        purchase.setSupplier(supplier);
        purchase.setReceivedBy(findUser(actor.userId()));
        purchase.setTotalCost(money(totalCost));
        purchase = purchaseRepository.save(purchase);
        List<PurchaseItem> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = lockedProducts.get(entry.getKey());
            int quantity = entry.getValue();
            product.setStockQty(safeStockAdd(product.getStockQty(), quantity));
            product.setCostPrice(unitCosts.get(entry.getKey()));
            PurchaseItem item = new PurchaseItem();
            item.setPurchase(purchase);
            item.setProduct(product);
            item.setQuantity(quantity);
            item.setUnitCost(unitCosts.get(entry.getKey()));
            items.add(item);
        }
        items = purchaseItemRepository.saveAll(items);
        for (PurchaseItem item : items) {
            writeMovement(item.getProduct(), item.getQuantity(), "PURCHASE", purchase.getId(), null, actor);
        }
        return PurchaseResponse.from(purchase, items);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public PageResponse<PurchaseResponse> listPurchases(int page, int size, AuthUserPrincipal actor) {
        requireRetail(actor);
        Page<Purchase> purchases = purchaseRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        Map<Long, List<PurchaseItem>> items = new HashMap<>();
        List<Long> ids = purchases.getContent().stream().map(Purchase::getId).toList();
        if (!ids.isEmpty()) {
            purchaseItemRepository.findByPurchaseIdIn(ids).forEach(item ->
                items.computeIfAbsent(item.getPurchase().getId(), ignored -> new ArrayList<>()).add(item));
        }
        return PageResponse.from(purchases.map(purchase -> PurchaseResponse.from(
            purchase, items.getOrDefault(purchase.getId(), List.of()))));
    }

    @Transactional(transactionManager = "tenantTransactionManager")
    public StockMovementResponse adjustStock(StockAdjustmentRequest request, AuthUserPrincipal actor) {
        requireRetail(actor);
        requireAny(actor, PURCHASE_ROLES);
        int change = request.changeQty();
        if (change == 0) throw new IllegalArgumentException("Stock adjustment cannot be zero");
        Product product = findProductForUpdate(request.productId());
        long nextStock = (long) product.getStockQty() + change;
        if (nextStock < 0) throw new ConflictException("Stock cannot be reduced below zero");
        if (nextStock > Integer.MAX_VALUE) throw new ConflictException("Stock quantity exceeds the supported limit");
        product.setStockQty((int) nextStock);
        productRepository.save(product);
        StockMovement movement = writeMovement(product, change, "ADJUSTMENT", null, request.note().trim(), actor);
        return StockMovementResponse.from(movement);
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public PageResponse<StockMovementResponse> listStockMovements(Long productId, int page, int size, AuthUserPrincipal actor) {
        requireRetail(actor);
        if (productId != null && !productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }
        Page<StockMovement> movements = productId == null
            ? stockMovementRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
            : stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId, PageRequest.of(page, size));
        return PageResponse.from(movements.map(StockMovementResponse::from));
    }

    @Transactional(transactionManager = "tenantTransactionManager", readOnly = true)
    public RetailDashboardResponse getDashboard(AuthUserPrincipal actor) {
        requireRetail(actor);
        LocalDate today = LocalDate.now();
        LocalDateTime startToday = today.atStartOfDay();
        LocalDateTime tomorrow = today.plusDays(1).atStartOfDay();
        LocalDateTime sevenDaysAgo = today.minusDays(6).atStartOfDay();

        List<Sale> todaysSales = saleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startToday, tomorrow);
        BigDecimal salesTotal = todaysSales.stream().map(Sale::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Sale> recentSales = saleRepository.findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(sevenDaysAgo, tomorrow);
        List<Long> recentIds = recentSales.stream().map(Sale::getId).toList();
        List<SaleItem> recentItems = recentIds.isEmpty() ? List.of() : saleItemRepository.findBySaleIdIn(recentIds);

        BigDecimal grossMarginToday = recentItems.stream()
            .filter(item -> item.getSale().getCreatedAt() != null
                && !item.getSale().getCreatedAt().isBefore(startToday)
                && item.getSale().getCreatedAt().isBefore(tomorrow))
            .map(item -> item.getUnitPrice().subtract(item.getUnitCost()).multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todaysDiscounts = todaysSales.stream().map(Sale::getDiscount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal todaysProfit = money(grossMarginToday.subtract(todaysDiscounts));

        Map<String, Long> quantityBySku = new HashMap<>();
        Map<String, Product> productBySku = new HashMap<>();
        recentItems.forEach(item -> {
            String sku = item.getProduct().getSku();
            quantityBySku.merge(sku, (long) item.getQuantity(), Long::sum);
            productBySku.putIfAbsent(sku, item.getProduct());
        });
        List<TopProductResponse> topProducts = quantityBySku.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()).thenComparing(Map.Entry.comparingByKey()))
            .limit(5)
            .map(entry -> new TopProductResponse(productBySku.get(entry.getKey()).getName(), entry.getKey(), entry.getValue()))
            .toList();

        Map<LocalDate, BigDecimal> salesByDate = new HashMap<>();
        recentSales.forEach(sale -> salesByDate.merge(sale.getCreatedAt().toLocalDate(), sale.getTotal(), BigDecimal::add));
        List<SalesPerDayResponse> salesPerDay = new ArrayList<>(7);
        for (int offset = 0; offset < 7; offset++) {
            LocalDate date = today.minusDays(6L - offset);
            salesPerDay.add(new SalesPerDayResponse(date, money(salesByDate.getOrDefault(date, BigDecimal.ZERO))));
        }
        return new RetailDashboardResponse(money(salesTotal), todaysProfit, todaysSales.size(),
            productRepository.findLowStock().size(), topProducts, salesPerDay);
    }

    private Map<Long, Integer> aggregateSaleQuantities(List<SaleLineRequest> lines) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (SaleLineRequest line : lines) {
            quantities.merge(line.productId(), line.quantity(), RetailService::safeQuantityAdd);
        }
        return quantities;
    }

    private Map<Long, Integer> aggregatePurchaseQuantities(List<PurchaseLineRequest> lines) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (PurchaseLineRequest line : lines) {
            quantities.merge(line.productId(), line.quantity(), RetailService::safeQuantityAdd);
        }
        return quantities;
    }

    private static int safeQuantityAdd(int left, int right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("Combined product quantity exceeds the supported limit");
        }
    }

    private Map<Long, Product> lockProducts(Set<Long> productIds) {
        Map<Long, Product> locked = new HashMap<>();
        productIds.stream().sorted().forEach(id -> locked.put(id, findProductForUpdate(id)));
        return locked;
    }

    private int safeStockAdd(int current, int change) {
        long next = (long) current + change;
        if (next > Integer.MAX_VALUE) throw new ConflictException("Stock quantity exceeds the supported limit");
        return (int) next;
    }

    private StockMovement writeMovement(
        Product product, int change, String reason, Long referenceId, String note, AuthUserPrincipal actor) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setChangeQty(change);
        movement.setReason(reason);
        movement.setReferenceId(referenceId);
        movement.setNote(note);
        movement.setCreatedBy(findUser(actor.userId()));
        return stockMovementRepository.save(movement);
    }

    private Map<Long, List<SaleItem>> saleItemsBySale(List<Long> saleIds) {
        Map<Long, List<SaleItem>> items = new HashMap<>();
        if (!saleIds.isEmpty()) {
            saleItemRepository.findBySaleIdIn(saleIds).forEach(item ->
                items.computeIfAbsent(item.getSale().getId(), ignored -> new ArrayList<>()).add(item));
        }
        return items;
    }

    private BigDecimal taxRate() {
        return settingsRepository.findByKey("tax_percent")
            .map(Settings::getValue)
            .filter(value -> value != null && !value.isBlank())
            .map(value -> {
                try {
                    BigDecimal rate = new BigDecimal(value);
                    if (rate.signum() < 0) throw new NumberFormatException("negative");
                    return rate;
                } catch (NumberFormatException ex) {
                    throw new IllegalStateException("Business tax setting is invalid", ex);
                }
            })
            .orElseThrow(() -> new IllegalStateException("Business tax setting is not configured"));
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
    }

    private void applyProductDetails(Product product, ProductRequest request) {
        product.setName(request.name().trim());
        if (request.categoryId() == null) {
            product.setCategory(null);
        } else {
            product.setCategory(findCategory(request.categoryId()));
        }
    }

    private void applyCustomer(Customer customer, CustomerRequest request) {
        customer.setName(request.name().trim());
        customer.setPhone(request.phone().trim());
        customer.setEmail(normalizeOptional(request.email()));
        customer.setAddress(normalizeOptional(request.address()));
    }

    private void applySupplier(Supplier supplier, SupplierRequest request) {
        supplier.setName(request.name().trim());
        supplier.setPhone(normalizeOptional(request.phone()));
        supplier.setAddress(normalizeOptional(request.address()));
    }

    private BigDecimal moneyOrZero(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO.setScale(2) : money(amount);
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean canManagePrices(AuthUserPrincipal actor) {
        return MANAGEMENT.contains(actor.role());
    }

    private void requireRetail(AuthUserPrincipal actor) {
        if (actor == null || !"RETAIL".equals(actor.businessType())) {
            throw new AccessDeniedException("Retail features are available only to retail businesses");
        }
    }

    private void requireAny(AuthUserPrincipal actor, String... roles) {
        for (String role : roles) if (role.equals(actor.role())) return;
        throw new AccessDeniedException("Your role cannot perform this retail operation");
    }

    private void requireAny(AuthUserPrincipal actor, Set<String> roles) {
        if (!roles.contains(actor.role())) throw new AccessDeniedException("Your role cannot perform this retail operation");
    }

    private Category findCategory(long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category not found"));
    }

    private Product findProductForUpdate(long id) {
        return productRepository.findByIdForUpdate(id).orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private Customer findCustomer(long id) {
        return customerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Customer not found"));
    }

    private User findUser(long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
