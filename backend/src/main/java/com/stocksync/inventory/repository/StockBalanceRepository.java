package com.stocksync.inventory.repository;
import com.stocksync.inventory.entity.StockBalance;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
public interface StockBalanceRepository extends JpaRepository<StockBalance,Long>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<StockBalance> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from StockBalance b where b.itemId=:itemId")
    Optional<StockBalance> findForUpdate(@Param("itemId")Long itemId);
}
