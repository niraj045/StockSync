package com.stocksync.challan.dto;

import java.math.BigDecimal;

public record ReceivingChallanItemResponse(
    Long id,
    Long itemId,
    String itemCode,
    String itemName,
    String size,
    String unit,
    Long linkedIssuedChallanItemId,
    Long openingImportTransactionId,
    BigDecimal pendingQuantitySnapshot,
    BigDecimal goodReturnedQuantity,
    BigDecimal damagedReturnedQuantity,
    BigDecimal lostQuantity,
    BigDecimal extraReturnedQuantity,
    Long exchangedFromItemId,
    String exchangedFromItemCode,
    Long exchangedToItemId,
    String exchangedToItemCode,
    BigDecimal exchangedQuantity,
    BigDecimal weightPerPieceSnapshot,
    BigDecimal goodReturnedWeight,
    BigDecimal damagedWeight,
    BigDecimal lostWeight,
    String notes,
    int sequence
) {}
