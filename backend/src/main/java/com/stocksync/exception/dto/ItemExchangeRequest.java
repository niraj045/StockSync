package com.stocksync.exception.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemExchangeRequest(
    Long id,
    
    @NotNull(message = "Agreement ID is required")
    Long agreementId,
    
    @NotNull(message = "Party ID is required")
    Long partyId,
    
    @NotNull(message = "Site ID is required")
    Long siteId,
    
    @NotNull(message = "Expected item ID is required")
    Long expectedItemId,
    
    @NotNull(message = "Actual item ID is required")
    Long actualItemId,
    
    @NotNull(message = "Exchange date is required")
    LocalDate exchangeDate,
    
    @NotNull(message = "Expected quantity is required")
    BigDecimal expectedQuantity,
    
    @NotNull(message = "Actual quantity is required")
    BigDecimal actualQuantity,
    
    @NotNull(message = "Expected weight is required")
    BigDecimal expectedWeight,
    
    @NotNull(message = "Actual weight is required")
    BigDecimal actualWeight,
    
    @NotNull(message = "Destination stock status is required")
    String destinationStockStatus, // AVAILABLE, DAMAGED
    
    @Size(max = 500)
    String reason
) {}
