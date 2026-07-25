package com.stocksync.inventory.dto;

import java.math.BigDecimal;

public record ItemResponse(Long id, String itemCode, String itemName, Long categoryId, String categoryName,
        String size, String unit, BigDecimal weightPerPiece, BigDecimal purchaseValue,
        String rentalConfiguration, BigDecimal lossRate, BigDecimal scrapValue,
        BigDecimal minimumStock, boolean active, long version) {}
