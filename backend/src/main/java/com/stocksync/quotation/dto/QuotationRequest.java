package com.stocksync.quotation.dto;

import com.stocksync.quotation.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record QuotationRequest(
        @NotNull Long quotationTemplateId,
        @NotNull Long partyId,
        @NotNull Long siteId,
        @NotNull LocalDate quotationDate,
        @NotNull LocalDate validUntil,
        @NotNull RentalType rentalType,
        @NotNull DiscountType discountType,
        @NotNull @DecimalMin("0") BigDecimal discountValue,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal cgstRate,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal sgstRate,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal igstRate,
        @NotNull @DecimalMin("0") BigDecimal transportCharge,
        @NotNull @DecimalMin("0") BigDecimal loadingCharge,
        @NotNull @DecimalMin("0") BigDecimal unloadingCharge,
        @NotNull @DecimalMin("0") BigDecimal otherCharge,
        @NotNull BigDecimal roundOff,
        @NotNull @DecimalMin("0") BigDecimal securityDeposit,
        @Size(max=4000) String terms,
        @Size(max=2000) String notes,
        @NotNull List<@Valid QuotationItemRequest> items,
        @Valid SteelFabExactHireRequest exactHire,
        Long version) {}
