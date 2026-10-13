package com.saas.platform.tenancy;

import java.sql.Connection;
import java.sql.SQLException;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private final TenantDataSourceRegistry registry;

    public TenantRoutingDataSource(TenantDataSourceRegistry registry) {
        this.registry = registry;
        setTargetDataSources(java.util.Collections.emptyMap());
        setLenientFallback(false);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            throw new TenantRoutingException("Tenant context is not set for this request");
        }
        return tenantId;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return registry.get((Long) determineCurrentLookupKey()).getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return registry.get((Long) determineCurrentLookupKey()).getConnection(username, password);
    }
}
