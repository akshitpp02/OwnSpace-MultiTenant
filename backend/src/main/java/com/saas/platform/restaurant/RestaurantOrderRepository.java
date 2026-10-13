package com.saas.platform.restaurant;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantOrderRepository extends JpaRepository<RestaurantOrder, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select restaurantOrder from RestaurantOrder restaurantOrder where restaurantOrder.id = :id")
    Optional<RestaurantOrder> findByIdForUpdate(@Param("id") long id);

    List<RestaurantOrder> findAllByOrderByCreatedAtDesc();
    List<RestaurantOrder> findByStatusInOrderByCreatedAtAsc(List<String> statuses);
    List<RestaurantOrder> findByStatusOrderByCreatedAtDesc(String status);
    List<RestaurantOrder> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime start, LocalDateTime end);
    long countByStatusIn(List<String> statuses);
    boolean existsByDiningTableIdAndStatusIn(Long tableId, List<String> statuses);
}
