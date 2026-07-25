package com.stocksync.order.repository;
import com.stocksync.order.entity.SiteOrder;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface SiteOrderRepository extends JpaRepository<SiteOrder,Long>,JpaSpecificationExecutor<SiteOrder>{
    @EntityGraph(attributePaths={"agreement","party","site","items","items.item"})
    Optional<SiteOrder>findDetailedById(Long id);
}
