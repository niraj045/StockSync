package com.stocksync.exception.repository;

import com.stocksync.exception.entity.StockDamage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface StockDamageRepository extends JpaRepository<StockDamage, Long>, JpaSpecificationExecutor<StockDamage> {

    @Query("select d from StockDamage d left join fetch d.sourceReceivingChallan left join fetch d.sourceReceivingChallanItem left join fetch d.agreement left join fetch d.party left join fetch d.site left join fetch d.item left join fetch d.attachment where d.id = :id")
    Optional<StockDamage> findByIdWithDetails(@Param("id") Long id);

    boolean existsBySourceReceivingChallanItemId(Long receivingChallanItemId);
}
