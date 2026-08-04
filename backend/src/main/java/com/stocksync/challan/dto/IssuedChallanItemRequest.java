package com.stocksync.challan.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record IssuedChallanItemRequest(
    @NotNull Long itemId,
    @NotNull @Positive BigDecimal quantity,
    @jakarta.validation.constraints.Size(max = 255) String notes
) {}
