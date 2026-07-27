package com.stocksync.exception.repository;

import com.stocksync.exception.entity.StockLoss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface StockLossRepository extends JpaRepository<StockLoss, Long>, JpaSpecificationExecutor<StockLoss> {
    
    @Query("select l from StockLoss l left join fetch l.sourceReceivingChallan left join fetch l.sourceReceivingChallanItem left join fetch l.agreement left join fetch l.party left join fetch l.site left join fetch l.item left join fetch l.attachment where l.id = :id")
    Optional<StockLoss> findByIdWithDetails(@Param("id") Long id);

    boolean existsBySourceReceivingChallanItemId(Long receivingChallanItemId);
}
