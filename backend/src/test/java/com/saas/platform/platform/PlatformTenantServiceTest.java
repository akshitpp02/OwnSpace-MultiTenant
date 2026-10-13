package com.saas.platform.platform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saas.platform.common.ResourceNotFoundException;
import com.saas.platform.tenancy.TenantDataSourceRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@SuppressWarnings("null")
class PlatformTenantServiceTest {

    @Test
    void suspendingTenantRemovesItsDatasourceAfterUpdatingStatus() {
        TenantRepository tenants = mock(TenantRepository.class);
        TenantDataSourceRegistry registry = mock(TenantDataSourceRegistry.class);
        Tenant tenant = tenant(12L, "ACTIVE");
        when(tenants.findById(12L)).thenReturn(Optional.of(tenant));
        when(tenants.save(tenant)).thenReturn(tenant);
        PlatformTenantService service = new PlatformTenantService(tenants, registry);

        TenantSummary result = service.changeTenantStatus(12L, "suspended");

        assertEquals("SUSPENDED", tenant.getStatus());
        assertEquals("SUSPENDED", result.status());
        verify(registry).remove(12L);
    }

    @Test
    void reactivatingTenantRegistersItsDatasource() {
        TenantRepository tenants = mock(TenantRepository.class);
        TenantDataSourceRegistry registry = mock(TenantDataSourceRegistry.class);
        Tenant tenant = tenant(12L, "SUSPENDED");
        when(tenants.findById(12L)).thenReturn(Optional.of(tenant));
        when(tenants.save(tenant)).thenReturn(tenant);
        PlatformTenantService service = new PlatformTenantService(tenants, registry);

        TenantSummary result = service.changeTenantStatus(12L, "active");

        assertEquals("ACTIVE", result.status());
        verify(registry).register(tenant);
    }

    @Test
    void statusChangeRejectsUnsupportedStatusesAndMissingTenants() {
        TenantRepository tenants = mock(TenantRepository.class);
        PlatformTenantService service = new PlatformTenantService(tenants, mock(TenantDataSourceRegistry.class));

        assertThrows(IllegalArgumentException.class, () -> service.changeTenantStatus(1L, "FAILED"));
        when(tenants.findById(2L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.changeTenantStatus(2L, "SUSPENDED"));
    }

    @Test
    void tenantListUsesCaseInsensitiveTypeFilterAndPaging() {
        TenantRepository tenants = mock(TenantRepository.class);
        TenantDataSourceRegistry registry = mock(TenantDataSourceRegistry.class);
        Tenant tenant = tenant(12L, "ACTIVE");
        when(tenants.findByFilters(any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(tenant), PageRequest.of(0, 10), 1));
        PlatformTenantService service = new PlatformTenantService(tenants, registry);

        var result = service.listTenants(null, "retail", 0, 10);

        assertEquals(1, result.totalElements());
        assertEquals("RETAIL", result.content().get(0).businessType());
    }

    @Test
    void statsAggregateTenantCountsByTypeAndStatus() {
        TenantRepository tenants = mock(TenantRepository.class);
        TenantDataSourceRegistry registry = mock(TenantDataSourceRegistry.class);
        TenantCountProjection retail = count("RETAIL", 3);
        TenantCountProjection active = count("ACTIVE", 2);
        TenantCountProjection suspended = count("SUSPENDED", 1);
        when(tenants.countTenantsByBusinessType()).thenReturn(List.of(retail));
        when(tenants.countTenantsByStatus()).thenReturn(List.of(active, suspended));
        when(tenants.count()).thenReturn(3L);
        PlatformTenantService service = new PlatformTenantService(tenants, registry);

        TenantStatsResponse stats = service.getStats();

        assertEquals(3L, stats.totalTenants());
        assertEquals(Map.of("RETAIL", 3L), stats.tenantsByType());
        assertEquals(Map.of("ACTIVE", 2L, "SUSPENDED", 1L), stats.tenantsByStatus());
    }

    private TenantCountProjection count(String name, long totalCount) {
        TenantCountProjection projection = mock(TenantCountProjection.class);
        when(projection.getName()).thenReturn(name);
        when(projection.getTotalCount()).thenReturn(totalCount);
        return projection;
    }

    private Tenant tenant(long id, String status) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setCode("sample-shop");
        tenant.setName("Sample Shop");
        tenant.setBusinessType("RETAIL");
        tenant.setDbName("tenant_" + id);
        tenant.setStatus(status);
        return tenant;
    }
}
