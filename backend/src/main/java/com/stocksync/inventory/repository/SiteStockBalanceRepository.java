package com.stocksync.inventory.repository;

import com.stocksync.inventory.entity.SiteStockBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface SiteStockBalanceRepository extends JpaRepository<SiteStockBalance, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SiteStockBalance s WHERE s.site.id = :siteId AND s.item.id = :itemId")
    Optional<SiteStockBalance> findForUpdate(@Param("siteId") Long siteId, @Param("itemId") Long itemId);

    Optional<SiteStockBalance> findBySiteIdAndItemId(Long siteId, Long itemId);

    java.util.List<SiteStockBalance> findBySiteIdAndPendingQuantityGreaterThan(Long siteId, java.math.BigDecimal pendingQuantity);

    org.springframework.data.domain.Page<SiteStockBalance> findBySiteId(Long siteId, org.springframework.data.domain.Pageable pageable);

    java.util.List<SiteStockBalance> findBySiteId(Long siteId);
}
