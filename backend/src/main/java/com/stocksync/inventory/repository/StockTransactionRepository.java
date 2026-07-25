package com.stocksync.inventory.repository;
import com.stocksync.inventory.entity.StockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface StockTransactionRepository extends JpaRepository<StockTransaction,Long>, JpaSpecificationExecutor<StockTransaction>{}
