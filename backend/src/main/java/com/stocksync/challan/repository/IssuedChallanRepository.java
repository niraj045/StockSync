package com.stocksync.challan.repository;

import com.stocksync.challan.entity.IssuedChallan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface IssuedChallanRepository extends JpaRepository<IssuedChallan, Long>, JpaSpecificationExecutor<IssuedChallan> {
    @EntityGraph(attributePaths = {"siteOrder", "items", "items.item"})
    Optional<IssuedChallan> findById(Long id);

    @Query("SELECT c FROM IssuedChallan c JOIN FETCH c.siteOrder o JOIN FETCH o.agreement a WHERE c.challanNumber LIKE :search OR o.orderNumber LIKE :search")
    Page<IssuedChallan> search(@Param("search") String search, Pageable pageable);

    java.util.List<IssuedChallan> findBySiteOrderSiteId(Long siteId);
}
