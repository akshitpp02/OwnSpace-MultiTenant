package com.saas.platform.config;

import com.saas.platform.tenancy.TenantDataSourceRegistry;
import com.saas.platform.tenancy.TenantRoutingDataSource;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@EnableJpaRepositories(
    basePackages = {
        "com.saas.platform.core",
        "com.saas.platform.restaurant",
        "com.saas.platform.retail"
    },
    entityManagerFactoryRef = "tenantEntityManagerFactory",
    transactionManagerRef = "tenantTransactionManager"
)
public class TenantDataSourceConfig {

    @Bean(name = "tenantRoutingDataSource")
    public DataSource tenantRoutingDataSource(TenantDataSourceRegistry registry) {
        return new TenantRoutingDataSource(registry);
    }

    @Bean(name = "tenantEntityManagerFactory")
    public LocalContainerEntityManagerFactoryBean tenantEntityManagerFactory(
        @Qualifier("tenantRoutingDataSource") DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan(
            "com.saas.platform.core",
            "com.saas.platform.restaurant",
            "com.saas.platform.retail"
        );
        factory.setPersistenceUnitName("tenant");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());

        Map<String, Object> jpaProperties = new HashMap<>();
        jpaProperties.put("hibernate.hbm2ddl.auto", "none");
        jpaProperties.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        jpaProperties.put("hibernate.boot.allow_jdbc_metadata_access", false);
        jpaProperties.put("hibernate.show_sql", false);
        jpaProperties.put("hibernate.format_sql", false);
        factory.setJpaPropertyMap(jpaProperties);
        return factory;
    }

    @Bean(name = "tenantTransactionManager")
    public PlatformTransactionManager tenantTransactionManager(
        @Qualifier("tenantEntityManagerFactory") LocalContainerEntityManagerFactoryBean factory) {
        return new JpaTransactionManager(factory.getObject());
    }
}
