package com.stocksync.agreement.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
public record QuotationConversionRequest(Long templateId,@NotNull LocalDate effectiveDate,LocalDate expiryDate,
        @NotNull @DecimalMin("0")BigDecimal securityDeposit,@Size(max=1000)String notes){}
