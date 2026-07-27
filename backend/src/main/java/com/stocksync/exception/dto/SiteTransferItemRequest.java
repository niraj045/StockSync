package com.stocksync.exception.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record SiteTransferItemRequest(
    @NotNull(message = "Source agreement item ID is required")
    Long sourceAgreementItemId,
    
    @NotNull(message = "Destination agreement item ID is required")
    Long destinationAgreementItemId,
    
    @NotNull(message = "Item ID is required")
    Long itemId,
    
    @NotNull(message = "Quantity is required")
    BigDecimal quantity,
    
    @NotNull(message = "Total weight is required")
    BigDecimal totalWeight
) {}
