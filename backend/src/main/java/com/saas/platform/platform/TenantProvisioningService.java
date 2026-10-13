package com.saas.platform.platform;

import com.saas.platform.common.ApiResponse;
import com.saas.platform.tenancy.BusinessType;
import com.saas.platform.tenancy.TenantDataSourceRegistry;
import com.saas.platform.tenancy.MigrationService;
import jakarta.validation.Valid;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TenantProvisioningService {

    private final TenantRepository tenantRepository;
    private final MigrationService migrationService;
    private final TenantDataSourceRegistry tenantDataSourceRegistry;
    private final PasswordEncoder passwordEncoder;
    private final String masterDbUrl;
    private final String masterDbUser;
    private final String masterDbPassword;

    public TenantProvisioningService(
        TenantRepository tenantRepository,
        MigrationService migrationService,
        TenantDataSourceRegistry tenantDataSourceRegistry,
        PasswordEncoder passwordEncoder,
        @Value("${master.datasource.url:jdbc:mysql://localhost:3306/master_db}") String masterDbUrl,
        @Value("${master.datasource.username:root}") String masterDbUser,
        @Value("${master.datasource.password:}") String masterDbPassword) {
        this.tenantRepository = tenantRepository;
        this.migrationService = migrationService;
        this.tenantDataSourceRegistry = tenantDataSourceRegistry;
        this.passwordEncoder = passwordEncoder;
        this.masterDbUrl = masterDbUrl;
        this.masterDbUser = masterDbUser;
        this.masterDbPassword = masterDbPassword;
    }

    @Transactional
    public ApiResponse<TenantRegistrationResponse> register(@Valid TenantRegistrationRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        String businessCode = request.getBusinessCode() == null ? null : request.getBusinessCode().trim();
        String businessTypeInput = request.getBusinessType() == null ? null : request.getBusinessType().trim();
        String businessName = request.getBusinessName() == null ? null : request.getBusinessName().trim();
        String ownerName = request.getOwnerName() == null ? null : request.getOwnerName().trim();
        String ownerEmail = request.getOwnerEmail() == null ? null : request.getOwnerEmail().trim();
        String ownerPassword = request.getOwnerPassword();

        if (businessCode == null || businessCode.isBlank()) {
            throw new IllegalArgumentException("Business code is required");
        }
        if (businessTypeInput == null || businessTypeInput.isBlank()) {
            throw new IllegalArgumentException("Business type is required");
        }

        BusinessType businessType;
        try {
            businessType = BusinessType.valueOf(businessTypeInput.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Business type must be RESTAURANT or RETAIL");
        }

        if (tenantRepository.findByCode(businessCode).isPresent()) {
            throw new TenantProvisioningException("Business code already exists");
        }

        Tenant tenant = new Tenant();
        tenant.setCode(businessCode);
        tenant.setName(businessName);
        tenant.setBusinessType(businessType.name());
        tenant.setStatus("PROVISIONING");
        Tenant savedTenant = tenantRepository.save(tenant);

        Long tenantId = savedTenant.getId();
        String dbName = "tenant_" + tenantId;

        try {
            savedTenant.setDbName(dbName);
            tenantRepository.save(savedTenant);

            validateDbName(dbName);
            createDatabase(dbName);
            migrationService.migrate(dbName, businessType);

            insertOwner(dbName, ownerName, ownerEmail, ownerPassword);
            insertDefaultSettings(dbName, businessType, businessName);

            savedTenant.setStatus("ACTIVE");
            tenantRepository.save(savedTenant);
            tenantDataSourceRegistry.register(savedTenant);

            return ApiResponse.success("Tenant registered successfully", new TenantRegistrationResponse(savedTenant.getId(), savedTenant.getCode(), savedTenant.getBusinessType()));
        } catch (Exception ex) {
            rollbackProvisioning(tenantId, dbName, savedTenant);
            throw new TenantProvisioningException("Tenant registration failed: " + ex.getMessage(), ex);
        }
    }

    private void rollbackProvisioning(Long tenantId, String dbName, Tenant tenant) {
        tenantDataSourceRegistry.remove(tenantId);
        try {
            dropDatabaseIfExists(dbName);
        } catch (Exception ignored) {
            // ignore cleanup errors to keep the failure response clean
        }

        if (tenant != null) {
            tenant.setStatus("FAILED");
            tenantRepository.save(tenant);
        }
    }

    private void validateDbName(String dbName) {
        if (dbName == null || !dbName.matches("^tenant_[0-9]+$")) {
            throw new IllegalArgumentException("Invalid database name generated for tenant");
        }
    }

    private void createDatabase(String dbName) throws SQLException {
        String jdbcUrl = toMasterJdbcUrl();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, masterDbUser, masterDbPassword);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS `" + dbName + "`");
        }
    }

    private void dropDatabaseIfExists(String dbName) throws SQLException {
        if (dbName == null) {
            return;
        }
        String jdbcUrl = toMasterJdbcUrl();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, masterDbUser, masterDbPassword);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS `" + dbName + "`");
        }
    }

    private void insertOwner(String dbName, String ownerName, String ownerEmail, String ownerPassword) throws SQLException {
        String tenantJdbcUrl = toJdbcUrl(dbName);
        try (Connection connection = DriverManager.getConnection(tenantJdbcUrl, masterDbUser, masterDbPassword)) {
            String insertSql = "INSERT INTO users (name, email, password_hash, role, active, created_at) VALUES (?, ?, ?, 'OWNER', true, CURRENT_TIMESTAMP)";
            try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
                statement.setString(1, ownerName);
                statement.setString(2, ownerEmail);
                statement.setString(3, passwordEncoder.encode(ownerPassword));
                statement.executeUpdate();
            }
        }
    }

    private void insertDefaultSettings(String dbName, BusinessType businessType, String businessName) throws SQLException {
        String tenantJdbcUrl = toJdbcUrl(dbName);
        try (Connection connection = DriverManager.getConnection(tenantJdbcUrl, masterDbUser, masterDbPassword)) {
            try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO settings (setting_key, setting_value) VALUES (?, ?), (?, ?)")) {
                statement.setString(1, "business_name");
                statement.setString(2, businessName);
                statement.setString(3, "tax_percent");
                statement.setString(4, businessType == BusinessType.RESTAURANT ? "5" : "0");
                statement.executeUpdate();
            }
        }
    }

    private String toMasterJdbcUrl() {
        return masterDbUrl;
    }

    private String toJdbcUrl(String dbName) {
        String url = masterDbUrl;
        if (url.contains("/")) {
            url = url.substring(0, url.lastIndexOf('/')); 
        }
        return url + "/" + dbName;
    }
}
