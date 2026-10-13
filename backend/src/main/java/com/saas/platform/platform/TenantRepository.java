package com.saas.platform.platform;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByCode(String code);
    List<Tenant> findByStatus(String status);

    @Query("""
        select tenant from Tenant tenant
        where (:status is null or upper(tenant.status) = :status)
          and (:businessType is null or upper(tenant.businessType) = :businessType)
        """)
    Page<Tenant> findByFilters(
        @Param("status") String status,
        @Param("businessType") String businessType,
        Pageable pageable
    );

    @Query("select tenant.businessType as name, count(tenant) as totalCount from Tenant tenant group by tenant.businessType")
    List<TenantCountProjection> countTenantsByBusinessType();

    @Query("select tenant.status as name, count(tenant) as totalCount from Tenant tenant group by tenant.status")
    List<TenantCountProjection> countTenantsByStatus();
}
