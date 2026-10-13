package com.saas.platform.tenancy;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;

class MigrationServiceTest {

    private static final String DB_URL = "jdbc:mysql://localhost:3306";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    @Test
    void migrateCreatesCoreAndDomainTablesForRestaurantAndRetail() throws Exception {
        MigrationService migrationService = new MigrationService("jdbc:mysql://localhost:3306/master_db", USER, PASSWORD);

        String restaurantDb = "tenant_test";
        String retailDb = "tenant_retail_test";

        try {
            createDatabaseIfMissing(restaurantDb);
            createDatabaseIfMissing(retailDb);

            migrationService.migrate(restaurantDb, BusinessType.RESTAURANT);
            assertTablesExist(restaurantDb, List.of("users", "settings", "categories", "menu_items", "dining_tables", "orders", "order_items", "bills"));

            migrationService.migrate(retailDb, BusinessType.RETAIL);
            assertTablesExist(retailDb, List.of("users", "settings", "categories", "products", "customers", "suppliers", "sales", "sale_items", "purchases", "purchase_items", "stock_movements"));
        } finally {
            dropDatabaseIfExists(restaurantDb);
            dropDatabaseIfExists(retailDb);
        }
    }

    private void createDatabaseIfMissing(String dbName) throws Exception {
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE IF NOT EXISTS `" + dbName + "`");
        }
    }

    private void dropDatabaseIfExists(String dbName) throws Exception {
        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASSWORD);
             Statement statement = connection.createStatement()) {
            statement.execute("DROP DATABASE IF EXISTS `" + dbName + "`");
        }
    }

    private void assertTablesExist(String dbName, List<String> expectedTables) throws Exception {
        try (Connection connection = DriverManager.getConnection(DB_URL + "/" + dbName, USER, PASSWORD);
             ResultSet resultSet = connection.getMetaData().getTables(dbName, null, "%", new String[] { "TABLE" })) {
            java.util.Set<String> actualTables = new java.util.HashSet<>();
            while (resultSet.next()) {
                actualTables.add(resultSet.getString("TABLE_NAME"));
            }

            for (String expectedTable : expectedTables) {
                assertTrue(actualTables.contains(expectedTable), "Missing table " + expectedTable + " in " + dbName + ", actual=" + actualTables);
            }
        }
    }
}
