package com.stocksync.inventory.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ItemRequest(
        @NotBlank @Size(max = 50) String itemCode,
        @NotBlank @Size(max = 150) String itemName,
        @NotNull Long categoryId,
        @Size(max = 100) String size,
        @NotBlank @Size(max = 30) String unit,
        @PositiveOrZero BigDecimal weightPerPiece,
        @PositiveOrZero BigDecimal purchaseValue,
        @Size(max = 500) String rentalConfiguration,
        @PositiveOrZero BigDecimal lossRate,
        @PositiveOrZero BigDecimal scrapValue,
        @NotNull @PositiveOrZero BigDecimal minimumStock,
        @NotNull Boolean active,
        Long version) {}
