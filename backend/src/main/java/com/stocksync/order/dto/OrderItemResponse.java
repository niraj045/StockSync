package com.stocksync.order.dto;
import java.math.BigDecimal;
public record OrderItemResponse(Long id,Long itemId,String itemCode,String itemName,String unit,BigDecimal orderedQuantity,
        BigDecimal issuedQuantity,BigDecimal remainingQuantity,long version){}
