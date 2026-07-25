package com.stocksync.inventory.dto;
import java.math.BigDecimal;
import java.time.LocalDate;
public record InventoryDocumentResponse(Long id,String number,String type,LocalDate date,BigDecimal totalValue,boolean replayed){}
