package com.stocksync.order.repository;
import com.stocksync.order.entity.SiteOrderItem;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
public interface SiteOrderItemRepository extends JpaRepository<SiteOrderItem,Long>{
    @Query("select coalesce(sum(i.orderedQuantity),0) from SiteOrderItem i where i.order.agreement.id=:agreementId and i.item.id=:itemId and i.order.status in (com.stocksync.order.entity.OrderStatus.CONFIRMED,com.stocksync.order.entity.OrderStatus.PARTIALLY_FULFILLED,com.stocksync.order.entity.OrderStatus.FULFILLED)")
    BigDecimal confirmedQuantity(@Param("agreementId")Long agreementId,@Param("itemId")Long itemId);
}
