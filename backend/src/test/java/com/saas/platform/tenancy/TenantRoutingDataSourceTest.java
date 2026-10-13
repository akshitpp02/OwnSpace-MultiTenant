package com.saas.platform.tenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantRoutingDataSourceTest {

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    @Test
    void refusesToRouteWithoutTenantContext() {
        TenantRoutingDataSource dataSource = new TenantRoutingDataSource(mock(TenantDataSourceRegistry.class));

        TenantRoutingException exception = assertThrows(TenantRoutingException.class, dataSource::getConnection);

        assertEquals("Tenant context is not set for this request", exception.getMessage());
    }

    @Test
    void getsConnectionFromTheContextTenantPool() throws Exception {
        TenantDataSourceRegistry registry = mock(TenantDataSourceRegistry.class);
        HikariDataSource tenantDataSource = mock(HikariDataSource.class);
        Connection connection = mock(Connection.class);
        when(registry.get(42L)).thenReturn(tenantDataSource);
        when(tenantDataSource.getConnection()).thenReturn(connection);
        TenantRoutingDataSource dataSource = new TenantRoutingDataSource(registry);
        TenantContext.set(42L);

        assertSame(connection, dataSource.getConnection());

        verify(registry).get(42L);
    }
}
