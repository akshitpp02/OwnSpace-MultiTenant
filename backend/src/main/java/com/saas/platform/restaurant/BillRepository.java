package com.saas.platform.restaurant;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByPaidAtGreaterThanEqualAndPaidAtLessThan(LocalDateTime start, LocalDateTime end);
    boolean existsByOrderId(Long orderId);
}
