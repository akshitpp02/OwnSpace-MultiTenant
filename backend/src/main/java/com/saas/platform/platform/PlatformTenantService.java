package com.saas.platform.platform;

import com.saas.platform.common.PageResponse;
import com.saas.platform.common.ResourceNotFoundException;
import com.saas.platform.tenancy.TenantDataSourceRegistry;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class PlatformTenantService {

    private final TenantRepository tenantRepository;
    private final TenantDataSourceRegistry tenantDataSourceRegistry;

    public PlatformTenantService(
        TenantRepository tenantRepository,
        TenantDataSourceRegistry tenantDataSourceRegistry) {
        this.tenantRepository = tenantRepository;
        this.tenantDataSourceRegistry = tenantDataSourceRegistry;
    }

    @Transactional(readOnly = true)
    public PageResponse<TenantSummary> listTenants(String status, String businessType, int page, int size) {
        String normalizedStatus = normalizeFilter(status, "status", "PROVISIONING", "ACTIVE", "SUSPENDED", "FAILED");
        String normalizedType = normalizeFilter(businessType, "business type", "RESTAURANT", "RETAIL");
        Page<Tenant> tenants = tenantRepository.findByFilters(
            normalizedStatus,
            normalizedType,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")))
        );
        return PageResponse.from(tenants.map(TenantSummary::from));
    }

    @Transactional
    public TenantSummary changeTenantStatus(long tenantId, String requestedStatus) {
        if (requestedStatus == null || requestedStatus.isBlank()) {
            throw new IllegalArgumentException("Tenant status is required");
        }
        String normalizedStatus = normalizeFilter(requestedStatus, "status", "ACTIVE", "SUSPENDED");
        Tenant tenant = tenantRepository.findById(tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Tenant not found"));

        if (normalizedStatus.equals(tenant.getStatus())) {
            return TenantSummary.from(tenant);
        }
        if (!"ACTIVE".equals(tenant.getStatus()) && !"SUSPENDED".equals(tenant.getStatus())) {
            throw new IllegalArgumentException("Only active or suspended tenants can change status");
        }

        tenant.setStatus(normalizedStatus);
        Tenant savedTenant = tenantRepository.save(tenant);
        updateRegistryAfterCommit(savedTenant, normalizedStatus);
        return TenantSummary.from(savedTenant);
    }

    @Transactional(readOnly = true)
    public TenantStatsResponse getStats() {
        Map<String, Long> byType = new LinkedHashMap<>();
        tenantRepository.countTenantsByBusinessType().forEach(row -> byType.put(row.getName(), row.getTotalCount()));
        Map<String, Long> byStatus = new LinkedHashMap<>();
        tenantRepository.countTenantsByStatus().forEach(row -> byStatus.put(row.getName(), row.getTotalCount()));
        return new TenantStatsResponse(tenantRepository.count(), byType, byStatus);
    }

    private String normalizeFilter(String value, String field, String... allowedValues) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (String allowed : allowedValues) {
            if (allowed.equals(normalized)) {
                return normalized;
            }
        }
        throw new IllegalArgumentException("Unsupported tenant " + field + ": " + value);
    }

    private void updateRegistryAfterCommit(Tenant tenant, String status) {
        Runnable update = () -> {
            if ("SUSPENDED".equals(status)) {
                tenantDataSourceRegistry.remove(tenant.getId());
            } else {
                tenantDataSourceRegistry.register(tenant);
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()
            && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    update.run();
                }
            });
        } else {
            update.run();
        }
    }
}
