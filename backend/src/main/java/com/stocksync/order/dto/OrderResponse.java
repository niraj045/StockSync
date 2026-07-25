package com.stocksync.order.dto;
import com.stocksync.order.entity.OrderStatus;
import java.time.*;
import java.util.List;
public record OrderResponse(Long id,String orderNumber,Long agreementId,String agreementNumber,Long partyId,String partyName,Long siteId,
        String siteName,LocalDate orderDate,OrderStatus status,String notes,List<OrderItemResponse>items,Long version,Instant createdAt,Instant updatedAt){}
