package com.stocksync.challan.repository;

import com.stocksync.challan.entity.ReceivingChallan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ReceivingChallanRepository extends JpaRepository<ReceivingChallan, Long>, JpaSpecificationExecutor<ReceivingChallan> {
    
    @EntityGraph(attributePaths = {"agreement", "party", "site", "linkedIssuedChallan", "items", "items.item"})
    Optional<ReceivingChallan> findById(Long id);

    @Query("SELECT c FROM ReceivingChallan c JOIN FETCH c.party p JOIN FETCH c.site s WHERE c.receivingChallanNumber LIKE :search OR p.legalName LIKE :search OR s.siteName LIKE :search")
    Page<ReceivingChallan> search(@Param("search") String search, Pageable pageable);
}
