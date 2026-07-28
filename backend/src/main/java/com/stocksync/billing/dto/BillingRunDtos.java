package com.stocksync.billing.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class BillingRunDtos {

    public record BillingRunRequest(
        @NotNull Long agreementId,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd
    ) {}

    public record BillingRunUpdateValuesRequest(
        BigDecimal manualAdjustmentTotal,
        String discountType,
        BigDecimal discountValue,
        List<Long> selectedChargeIds
    ) {}

    public record EligibleAgreementResponse(
        Long id,
        String agreementNumber,
        String partyName,
        String siteName,
        LocalDate effectiveDate,
        LocalDate expiryDate
    ) {}

    public record BillingRunResponse(
        Long id,
        String billingRunNumber,
        Long agreementId,
        String agreementNumber,
        Long partyId,
        String partyName,
        Long siteId,
        String siteName,
        LocalDate periodStart,
        LocalDate periodEnd,
        String status,
        BigDecimal rentalSubtotal,
        BigDecimal lossChargeTotal,
        BigDecimal damageChargeTotal,
        BigDecimal operationalChargeTotal,
        BigDecimal manualAdjustmentTotal,
        String discountType,
        BigDecimal discountValue,
        BigDecimal discountAmount,
        BigDecimal taxableAmount,
        BigDecimal cgstRate,
        BigDecimal cgstAmount,
        BigDecimal sgstRate,
        BigDecimal sgstAmount,
        BigDecimal igstRate,
        BigDecimal igstAmount,
        BigDecimal totalTax,
        BigDecimal roundOff,
        BigDecimal grandTotal,
        Instant calculatedAt,
        String calculatedBy,
        Instant finalizedAt,
        String finalizedBy,
        Instant cancelledAt,
        String cancelledBy,
        String cancellationReason,
        long version,
        List<BillingRunSegmentResponse> segments,
        List<BillingRunChargeResponse> charges
    ) {}

    public record BillingRunSegmentResponse(
        Long id,
        Long agreementItemId,
        Long itemId,
        String itemCode,
        String itemName,
        String size,
        String unit,
        BigDecimal weight,
        String sourceIssueReference,
        String sourceEndReference,
        String rentalType,
        BigDecimal quantity,
        BigDecimal area,
        BigDecimal weightVal,
        LocalDate segmentStart,
        LocalDate segmentEnd,
        int billableDays,
        BigDecimal baseRate,
        String appliedSlab,
        BigDecimal amount,
        String calculationExplanation,
        int sequenceNumber
    ) {}

    public record BillingRunChargeResponse(
        Long id,
        String sourceType,
        Long sourceId,
        String sourceDocumentNumber,
        String chargeType,
        String description,
        BigDecimal quantity,
        BigDecimal rate,
        BigDecimal amount,
        boolean taxable,
        boolean selected
    ) {}
}
