package com.stocksync.quotation.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record QuotationItemRequest(
        @NotNull Long itemId,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal quantity,
        @NotNull @DecimalMin(value = "0") BigDecimal unitRate,
        @NotNull @DecimalMin(value = "0") BigDecimal rentalRate,
        @Size(max = 500) String notes) {}
