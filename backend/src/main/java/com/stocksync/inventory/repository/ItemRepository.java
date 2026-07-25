package com.stocksync.inventory.repository;

import com.stocksync.inventory.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {
    boolean existsByItemCodeIgnoreCase(String code);
    boolean existsByItemCodeIgnoreCaseAndIdNot(String code, Long id);
    boolean existsByCategoryId(Long categoryId);
}
