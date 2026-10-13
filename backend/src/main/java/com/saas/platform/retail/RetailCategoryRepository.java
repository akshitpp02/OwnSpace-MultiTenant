package com.saas.platform.retail;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RetailCategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByNameIgnoreCase(String name);
}
