package com.saas.platform.retail;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {
    List<SaleItem> findBySaleIdOrderByIdAsc(Long saleId);
    List<SaleItem> findBySaleIdIn(List<Long> saleIds);
}
