package com.stocksync.challan.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ReceivingChallanItemRequest(
    @NotNull Long itemId,
    Long linkedIssuedChallanItemId,
    Long openingImportTransactionId,
    BigDecimal goodReturnedQuantity,
    BigDecimal damagedReturnedQuantity,
    BigDecimal lostQuantity,
    BigDecimal extraReturnedQuantity,
    Long exchangedFromItemId,
    Long exchangedToItemId,
    BigDecimal exchangedQuantity,
    String notes
) {}
