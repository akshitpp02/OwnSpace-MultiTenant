package com.saas.platform.tenancy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.saas.platform.platform.Tenant;
import com.saas.platform.platform.TenantRepository;
import org.junit.jupiter.api.Test;

class TenantDataSourceRegistryTest {

    @Test
    void migratesEachActiveTenantAndContinuesAfterOneTenantFails() {
        Tenant firstTenant = tenant(1L, "tenant_1", "RESTAURANT");
        Tenant secondTenant = tenant(2L, "tenant_2", "RETAIL");
        TenantRepository tenantRepository = mock(TenantRepository.class);
        MigrationService migrationService = mock(MigrationService.class);
        when(tenantRepository.findByStatus("ACTIVE")).thenReturn(java.util.List.of(firstTenant, secondTenant));
        doThrow(new IllegalStateException("Migration failed"))
            .when(migrationService).migrate("tenant_1", BusinessType.RESTAURANT);

        TenantDataSourceRegistry registry = org.mockito.Mockito.spy(new TenantDataSourceRegistry(
            tenantRepository, migrationService, "jdbc:mysql://localhost:3306/master_db", "root", "root"));
        doNothing().when(registry).register(any(Tenant.class));

        registry.run(null);

        verify(migrationService).migrate("tenant_1", BusinessType.RESTAURANT);
        verify(migrationService).migrate("tenant_2", BusinessType.RETAIL);
        verify(registry).register(secondTenant);
        verify(tenantRepository).findByStatus("ACTIVE");
    }

    private Tenant tenant(Long id, String databaseName, String businessType) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setCode("TENANT_" + id);
        tenant.setBusinessType(businessType);
        tenant.setDbName(databaseName);
        tenant.setStatus("ACTIVE");
        return tenant;
    }
}
