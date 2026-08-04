package com.stocksync.billing.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class InvoiceDtos {

    public record InvoiceUpdateRequest(
        @NotNull LocalDate dueDate,
        String terms,
        String notes,
        @NotNull Long version
    ) {}

    public record InvoiceCancelRequest(
        @NotBlank String reason
    ) {}

    public record InvoiceResponse(
        Long id,
        String invoiceNumber,
        Long billingRunId,
        String billingRunNumber,
        Long agreementId,
        String agreementNumberSnapshot,
        String measurementBasis,
        String companyNameSnapshot,
        String companyAddressSnapshot,
        String companyGstinSnapshot,
        String partyNameSnapshot,
        String partyGstinSnapshot,
        String partyAddressSnapshot,
        String partyStateSnapshot,
        String siteNameSnapshot,
        String siteCodeSnapshot,
        String siteAddressSnapshot,
        String siteContactSnapshot,
        LocalDate invoiceDate,
        LocalDate dueDate,
        LocalDate periodStart,
        LocalDate periodEnd,
        String status,
        BigDecimal subtotal,
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
        BigDecimal cashAllocatedTotal,
        BigDecimal tdsAllocatedTotal,
        BigDecimal depositAdjustedTotal,
        BigDecimal outstandingAmount,
        String paymentStatus,
        String terms,
        String notes,
        Long generatedPdfAttachmentId,
        Instant issuedAt,
        String issuedBy,
        Instant cancelledAt,
        String cancelledBy,
        String cancellationReason,
        long version,
        List<InvoiceItemResponse> items
    ) {}

    public record InvoiceItemResponse(
        Long id,
        String lineType,
        Long agreementItemId,
        String sourceType,
        Long sourceId,
        String sourceDocumentNumber,
        Long itemId,
        String itemCodeSnapshot,
        String itemNameSnapshot,
        String sizeSnapshot,
        String unitSnapshot,
        String description,
        BigDecimal quantity,
        BigDecimal area,
        BigDecimal weight,
        Integer billableDays,
        BigDecimal rate,
        boolean taxable,
        BigDecimal amount,
        int sequenceNumber
    ) {}
}
