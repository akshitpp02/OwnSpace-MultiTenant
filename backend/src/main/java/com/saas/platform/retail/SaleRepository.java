package com.saas.platform.retail;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, Long> {
    @Query("""
        select sale from Sale sale
        where (:start is null or sale.createdAt >= :start)
          and (:end is null or sale.createdAt < :end)
          and (:cashierId is null or sale.cashier.id = :cashierId)
        order by sale.createdAt desc
        """)
    Page<Sale> findFiltered(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        @Param("cashierId") Long cashierId,
        Pageable pageable);

    List<Sale> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);
}
