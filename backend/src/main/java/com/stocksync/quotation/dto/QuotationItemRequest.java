package com.stocksync.quotation.dto;

import com.stocksync.quotation.entity.RentalType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record QuotationItemRequest(
        @NotNull Long itemId,
        @NotNull @DecimalMin(value="0.0001") BigDecimal quantity,
        @NotNull @DecimalMin(value="0.0001") BigDecimal rate,
        @DecimalMin("0") BigDecimal requiredQuantity,
        @DecimalMin(value="0.01") BigDecimal hireMonths,
        @DecimalMin("0") BigDecimal replacementRate,
        @NotNull RentalType rentalType,
        @DecimalMin("0") BigDecimal area,
        @DecimalMin("0") BigDecimal weight,
        @Size(max=500) String description) {}
