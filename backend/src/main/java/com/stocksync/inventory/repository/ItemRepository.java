package com.stocksync.inventory.repository;

import com.stocksync.inventory.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long>, JpaSpecificationExecutor<Item> {
    boolean existsByItemCodeIgnoreCase(String code);
    boolean existsByItemCodeIgnoreCaseAndIdNot(String code, Long id);
    boolean existsByCategoryId(Long categoryId);
    Optional<Item> findByItemCodeIgnoreCase(String code);
    Optional<Item> findByItemNameIgnoreCase(String name);
}
