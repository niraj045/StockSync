package com.stocksync.inventory.dto;
import java.math.BigDecimal;
import java.time.Instant;
public record StockBalanceResponse(Long itemId,String itemCode,String itemName,String categoryName,String unit,
        BigDecimal availableQuantity,BigDecimal issuedQuantity,BigDecimal hiredQuantity,BigDecimal lostQuantity,
        BigDecimal scrappedQuantity,BigDecimal availableWeight,BigDecimal minimumStock,boolean belowMinimum,long version,Instant updatedAt){}
