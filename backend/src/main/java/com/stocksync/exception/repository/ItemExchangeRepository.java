package com.stocksync.exception.repository;

import com.stocksync.exception.entity.ItemExchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ItemExchangeRepository extends JpaRepository<ItemExchange, Long>, JpaSpecificationExecutor<ItemExchange> {

    @Query("select e from ItemExchange e left join fetch e.sourceReceivingChallan left join fetch e.sourceReceivingChallanItem left join fetch e.agreement left join fetch e.party left join fetch e.site left join fetch e.expectedItem left join fetch e.actualItem where e.id = :id")
    Optional<ItemExchange> findByIdWithDetails(@Param("id") Long id);

    boolean existsBySourceReceivingChallanItemId(Long receivingChallanItemId);
}
