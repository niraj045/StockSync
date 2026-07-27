package com.stocksync.challan.dto;

import java.math.BigDecimal;

public record SiteStockBalanceResponse(
    Long id,
    Long itemId,
    String itemCode,
    String itemName,
    String size,
    String unit,
    BigDecimal pendingQuantity
) {}
