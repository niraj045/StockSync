package com.stocksync.quotation.dto;

import com.stocksync.quotation.entity.RentalType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record QuotationRequest(
        @NotNull Long partyId,
        @NotNull Long siteId,
        @NotNull LocalDate quotationDate,
        @NotNull LocalDate validUntil,
        @NotNull RentalType rentalType,
        @NotNull @DecimalMin("0") BigDecimal transportCharge,
        @NotNull @DecimalMin("0") BigDecimal loadingCharge,
        @NotNull @DecimalMin("0") BigDecimal unloadingCharge,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal taxRate,
        @Size(max = 4000) String terms,
        @Size(max = 1000) String notes,
        @NotEmpty List<@Valid QuotationItemRequest> items,
        Long version) {}
