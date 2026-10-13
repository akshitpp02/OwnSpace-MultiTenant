package com.saas.platform.tenancy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MigrationService {

    private final String mysqlHost;
    private final String mysqlUser;
    private final String mysqlPassword;

    public MigrationService(
        @Value("${master.datasource.url:jdbc:mysql://localhost:3306/master_db}") String masterUrl,
        @Value("${master.datasource.username:root}") String mysqlUser,
        @Value("${master.datasource.password:}") String mysqlPassword) {
        this.mysqlUser = mysqlUser;
        this.mysqlPassword = mysqlPassword;

        String normalized = masterUrl;
        if (normalized.contains("/")) {
            normalized = normalized.substring(0, normalized.lastIndexOf('/'));
        }
        this.mysqlHost = normalized;
    }

    public void migrate(String dbName, BusinessType type) {
        if (dbName == null || !dbName.matches("^tenant_[0-9]+$|^tenant_test$|^tenant_[A-Za-z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid database name: " + dbName);
        }

        String databaseUrl = mysqlHost + "/" + dbName;

        Flyway flyway = Flyway.configure()
            .dataSource(databaseUrl, mysqlUser, mysqlPassword)
            .locations("classpath:db/core", "classpath:db/" + type.name().toLowerCase())
            .baselineOnMigrate(true)
            .load();

        flyway.migrate();
    }

    public void createDatabaseIfMissing(String dbName) throws SQLException {
        String jdbcUrl = mysqlHost.replace("jdbc:mysql:", "jdbc:mysql:");
        try (Connection connection = DriverManager.getConnection(jdbcUrl, mysqlUser, mysqlPassword)) {
            String sql = "CREATE DATABASE IF NOT EXISTS `" + dbName + "`";
            try (var stmt = connection.createStatement()) {
                stmt.execute(sql);
            }
        }
    }
}
