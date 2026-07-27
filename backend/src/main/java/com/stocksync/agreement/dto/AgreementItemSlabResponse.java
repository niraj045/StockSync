package com.stocksync.agreement.dto;

import java.math.BigDecimal;

public record AgreementItemSlabResponse(
    Long id,
    int startDay,
    Integer endDay,
    BigDecimal rate
) {}
