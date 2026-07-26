package com.stocksync.inventory.repository;

import com.stocksync.inventory.entity.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface ItemCategoryRepository extends JpaRepository<ItemCategory, Long>, JpaSpecificationExecutor<ItemCategory> {
    Optional<ItemCategory> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
