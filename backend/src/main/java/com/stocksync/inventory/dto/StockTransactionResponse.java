package com.stocksync.inventory.dto;
import java.math.BigDecimal;
import java.time.*;
public record StockTransactionResponse(Long id,Long itemId,String itemCode,String itemName,String transactionType,
        LocalDate transactionDate,BigDecimal quantity,BigDecimal weight,String direction,String sourceType,
        Long sourceId,String notes,String createdBy,Instant createdAt){}
