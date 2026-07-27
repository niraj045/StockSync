package com.stocksync.agreement.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AgreementItemSlabRequest(
    @NotNull @Min(1) Integer startDay,
    Integer endDay,
    @NotNull @DecimalMin("0") BigDecimal rate
) {}
