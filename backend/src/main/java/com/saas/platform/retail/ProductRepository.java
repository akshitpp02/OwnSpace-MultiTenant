package com.saas.platform.retail;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsByCategoryId(Long categoryId);

    @Query("""
        select product from Product product
        where (:categoryId is null or product.category.id = :categoryId)
          and (:active is null or product.active = :active)
          and (:term is null or lower(product.name) like lower(concat('%', :term, '%'))
               or lower(product.sku) like lower(concat('%', :term, '%')))
        """)
    Page<Product> search(
        @Param("term") String term,
        @Param("categoryId") Long categoryId,
        @Param("active") Boolean active,
        Pageable pageable);

    @Query("select product from Product product where product.stockQty <= product.lowStockThreshold order by product.stockQty asc, product.name asc")
    List<Product> findLowStock();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select product from Product product where product.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") long id);
}
