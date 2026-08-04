package com.stocksync.challan.dto;

import java.math.BigDecimal;

public record IssuedChallanItemResponse(
    Long id,
    Long itemId,
    String itemCode,
    String itemName,
    String unit,
    BigDecimal quantity,
    String notes
) {}
