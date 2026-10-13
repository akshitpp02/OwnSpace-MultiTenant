package com.saas.platform.restaurant;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select diningTable from DiningTable diningTable where diningTable.id = :id")
    Optional<DiningTable> findByIdForUpdate(@Param("id") long id);

    boolean existsByNameIgnoreCase(String name);
}
