package com.stocksync.exception.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record StockDamageRequest(
    Long id,
    
    @NotNull(message = "Agreement ID is required")
    Long agreementId,
    
    @NotNull(message = "Party ID is required")
    Long partyId,
    
    @NotNull(message = "Site ID is required")
    Long siteId,
    
    @NotNull(message = "Item ID is required")
    Long itemId,
    
    @NotNull(message = "Damage date is required")
    LocalDate damageDate,
    
    BigDecimal quantity,
    BigDecimal weight,
    
    boolean repairable,
    
    @NotNull(message = "Damage type is required")
    String damageType,
    
    @Size(max = 500)
    String conditionNotes,
    
    @NotNull(message = "Charge method is required")
    String chargeMethod,
    
    BigDecimal damageRate,
    BigDecimal estimatedRepairCost,
    BigDecimal actualRepairCost,
    
    Long attachmentId
) {}
