package com.stocksync.agreement.dto;
import com.stocksync.agreement.entity.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AgreementRequest(
 @NotNull LocalDate agreementDate,@NotNull LocalDate effectiveDate,LocalDate expiryDate,@NotNull BillingCycle billingCycle,
 @Positive Integer customBillingCycleDays,@PositiveOrZero Integer gracePeriodDays,@PositiveOrZero Integer minimumBillingDays,
 @NotNull @DecimalMin("0") BigDecimal securityDeposit,@Size(max=2000) String headerText,@Size(max=255) String partATitle,String partAText,@Size(max=255) String partBTitle,@Size(max=4000) String terms,@Size(max=1000) String notes,
 @NotEmpty List<@Valid AgreementItemRequest> items,@NotNull Long version,
 String billingStartRule,String billingEndRule,MeasurementBasis measurementBasis,
 BillingCommencementRule billingCommencementRule,LocalDate fixedBillingStartDate){}
