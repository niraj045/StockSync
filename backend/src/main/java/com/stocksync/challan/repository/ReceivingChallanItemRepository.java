package com.stocksync.challan.repository;

import com.stocksync.challan.entity.ReceivingChallanItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceivingChallanItemRepository extends JpaRepository<ReceivingChallanItem, Long> {
}
