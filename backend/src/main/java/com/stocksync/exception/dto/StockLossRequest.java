package com.stocksync.exception.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record StockLossRequest(
    Long id,
    
    @NotNull(message = "Agreement ID is required")
    Long agreementId,
    
    @NotNull(message = "Party ID is required")
    Long partyId,
    
    @NotNull(message = "Site ID is required")
    Long siteId,
    
    @NotNull(message = "Item ID is required")
    Long itemId,
    
    @NotNull(message = "Loss date is required")
    LocalDate lossDate,
    
    BigDecimal quantity,
    BigDecimal weight,
    
    @NotNull(message = "Charge method is required")
    String chargeMethod,
    
    BigDecimal recoveryRate,
    
    @Size(max = 500)
    String reason,
    
    Long attachmentId
) {}
