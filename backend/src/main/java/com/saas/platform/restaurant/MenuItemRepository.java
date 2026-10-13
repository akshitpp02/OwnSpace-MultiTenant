package com.saas.platform.restaurant;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    Page<MenuItem> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<MenuItem> findByCategoryId(Long categoryId, Pageable pageable);
    Page<MenuItem> findByCategoryIdAndNameContainingIgnoreCase(Long categoryId, String name, Pageable pageable);
    List<MenuItem> findByIdIn(List<Long> ids);
    boolean existsByCategoryId(Long categoryId);
}
