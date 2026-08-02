package com.stocksync.quotation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SteelFabExactHireRequest(
        @NotBlank @Size(max = 500) String partyAddress,
        @NotBlank @Size(max = 300) String subject,
        @NotNull @Min(1) @Max(365) Integer validityDays,
        @NotBlank @Size(max = 80) String minimumHirePeriod,
        @NotNull @Min(1) @Max(3650) Integer minimumHireDays,
        @NotNull @DecimalMin("0.0001") BigDecimal siteLengthRmt,
        @NotNull @DecimalMin("0.0001") BigDecimal siteHeightMtr,
        @NotNull @DecimalMin("0") BigDecimal gstPercentage,
        @NotNull @DecimalMin("0") BigDecimal advanceRent,
        @NotNull @Min(0) @Max(365) Integer paymentDueDays,
        @NotBlank @Size(max = 120) String authorizedPerson,
        @NotBlank @Size(max = 120) String authorizedDesignation,
        @NotBlank @Size(max = 30) String authorizedPhone,
        @Size(max = 120) String acceptedBy,
        @Size(max = 120) String acceptedDesignation,
        @Size(max = 30) String acceptedPhone,
        LocalDate acceptedDate) {}
