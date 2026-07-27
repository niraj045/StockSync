package com.stocksync.exception.repository;

import com.stocksync.exception.entity.SiteTransferItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SiteTransferItemRepository extends JpaRepository<SiteTransferItem, Long> {
    List<SiteTransferItem> findByTransferId(Long transferId);
}
