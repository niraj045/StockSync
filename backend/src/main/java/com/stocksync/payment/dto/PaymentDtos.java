package com.stocksync.payment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class PaymentDtos {
    public record AllocationRequest(@NotNull Long invoiceId, @NotNull @DecimalMin("0.00") BigDecimal cashAllocated, @NotNull @DecimalMin("0.00") BigDecimal tdsAllocated) {}
    public record PaymentRequest(
            @NotNull Long partyId,
            Long siteId,
            @NotNull LocalDate paymentDate,
            @NotBlank String paymentMode,
            String referenceNumber,
            String bankName,
            String chequeNumber,
            LocalDate chequeDate,
            @NotNull @DecimalMin("0.00") BigDecimal cashAmount,
            @NotNull @DecimalMin("0.00") BigDecimal tdsAmount,
            String notes,
            @Valid List<AllocationRequest> allocations,
            Long version) {}
    public record AllocateRequest(@NotEmpty @Valid List<AllocationRequest> allocations) {}
    public record ReasonRequest(@NotBlank String reason) {}
    public record TdsDetailsRequest(LocalDate deductionDate, String section, String certificateNumber, LocalDate certificateDate, Long certificateAttachmentId) {}
    public record TdsRejectRequest(@NotBlank String reason) {}
    public record PaymentAllocationResponse(Long id, Long invoiceId, String invoiceNumber, BigDecimal cashAllocated, BigDecimal tdsAllocated, BigDecimal totalAllocated) {}
    public record TdsDetailsResponse(Long id, BigDecimal tdsAmount, LocalDate deductionDate, String section, String certificateNumber, LocalDate certificateDate, Long certificateAttachmentId, String verificationStatus, String rejectionReason, Instant verifiedAt, String verifiedBy, long version) {}
    public record PaymentResponse(
            Long id, String receiptNumber, Long partyId, String partyName, Long siteId, String siteName,
            LocalDate paymentDate, String paymentMode, String referenceNumber, String bankName, String chequeNumber, LocalDate chequeDate,
            BigDecimal cashAmount, BigDecimal tdsAmount, BigDecimal totalSettlementAmount, BigDecimal unallocatedAmount,
            String status, String notes, Long attachmentId, Instant postedAt, String postedBy, Instant reversedAt, String reversedBy, String reversalReason,
            Instant createdAt, String createdBy, Instant updatedAt, String updatedBy, long version,
            List<PaymentAllocationResponse> allocations, TdsDetailsResponse tdsDetails) {}
    public record EligibleInvoiceResponse(Long id, String invoiceNumber, Long agreementId, String agreementNumber, Long siteId, String siteName, LocalDate invoiceDate, LocalDate dueDate, BigDecimal grandTotal, BigDecimal outstandingAmount, String paymentStatus) {}
    public record AdvanceResponse(Long partyId, BigDecimal availableAdvance) {}
}
