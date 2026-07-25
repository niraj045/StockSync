package com.stocksync.agreement.dto;
import com.stocksync.quotation.entity.RentalType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
public record AgreementRequest(@NotNull Long partyId,@NotNull Long siteId,Long templateId,@NotNull LocalDate effectiveDate,
        LocalDate expiryDate,@NotNull RentalType rentalType,@NotNull @DecimalMin("0")BigDecimal securityDeposit,
        @NotNull @DecimalMin("0")BigDecimal transportCharge,@NotNull @DecimalMin("0")BigDecimal loadingCharge,
        @NotNull @DecimalMin("0")BigDecimal unloadingCharge,@Size(max=4000)String terms,@Size(max=1000)String notes,
        @NotEmpty List<@Valid AgreementItemRequest>items,Long version){}
