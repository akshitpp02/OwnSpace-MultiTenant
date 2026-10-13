package com.saas.platform.tenancy;

import com.saas.platform.platform.Tenant;
import com.saas.platform.platform.TenantRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class TenantDataSourceRegistry implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(TenantDataSourceRegistry.class);

    private final Map<Long, HikariDataSource> dataSources = new ConcurrentHashMap<>();
    private final TenantRepository tenantRepository;
    private final MigrationService migrationService;
    private final String masterDbUrl;
    private final String masterDbUser;
    private final String masterDbPassword;

    public TenantDataSourceRegistry(
        TenantRepository tenantRepository,
        MigrationService migrationService,
        @Value("${master.datasource.url:jdbc:mysql://localhost:3306/master_db}") String masterDbUrl,
        @Value("${master.datasource.username:root}") String masterDbUser,
        @Value("${master.datasource.password:}") String masterDbPassword) {
        this.tenantRepository = tenantRepository;
        this.migrationService = migrationService;
        this.masterDbUrl = masterDbUrl;
        this.masterDbUser = masterDbUser;
        this.masterDbPassword = masterDbPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (Tenant tenant : tenantRepository.findByStatus("ACTIVE")) {
            try {
                BusinessType businessType = BusinessType.valueOf(tenant.getBusinessType());
                migrationService.migrate(tenant.getDbName(), businessType);
                register(tenant);
            } catch (Exception ex) {
                LOGGER.error("Failed to initialize active tenant {} ({})",
                    tenant.getId(), tenant.getCode(), ex);
            }
        }
    }

    public void register(Tenant tenant) {
        if (tenant == null || tenant.getId() == null || !"ACTIVE".equals(tenant.getStatus())) {
            throw new IllegalArgumentException("Only an active tenant with an id can be registered");
        }

        String expectedDbName = "tenant_" + tenant.getId();
        if (!expectedDbName.equals(tenant.getDbName())) {
            throw new IllegalArgumentException("Tenant database name does not match its tenant id");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(databaseUrl(expectedDbName));
        config.setUsername(masterDbUser);
        config.setPassword(masterDbPassword);
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setPoolName("tenant-" + tenant.getId());

        HikariDataSource candidate = new HikariDataSource(config);
        HikariDataSource existing = dataSources.putIfAbsent(tenant.getId(), candidate);
        if (existing != null) {
            candidate.close();
        }
    }

    public HikariDataSource get(Long tenantId) {
        if (tenantId == null) {
            throw new TenantRoutingException("Tenant id is required to access tenant data");
        }
        HikariDataSource dataSource = dataSources.get(tenantId);
        if (dataSource == null) {
            throw new TenantRoutingException("Tenant " + tenantId + " is not active or is not registered");
        }
        return dataSource;
    }

    public void remove(Long tenantId) {
        HikariDataSource dataSource = dataSources.remove(tenantId);
        if (dataSource != null) {
            dataSource.close();
        }
    }

    private String databaseUrl(String databaseName) {
        int queryIndex = masterDbUrl.indexOf('?');
        String query = queryIndex >= 0 ? masterDbUrl.substring(queryIndex) : "";
        String urlWithoutQuery = queryIndex >= 0 ? masterDbUrl.substring(0, queryIndex) : masterDbUrl;
        int databaseSeparator = urlWithoutQuery.lastIndexOf('/');
        if (databaseSeparator < "jdbc:mysql://".length()) {
            throw new IllegalArgumentException("Invalid master MySQL JDBC URL");
        }
        return urlWithoutQuery.substring(0, databaseSeparator + 1) + databaseName + query;
    }

    @PreDestroy
    public void closeAll() {
        dataSources.values().forEach(HikariDataSource::close);
        dataSources.clear();
    }
}
