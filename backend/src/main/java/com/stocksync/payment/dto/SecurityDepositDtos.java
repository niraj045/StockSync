package com.stocksync.payment.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public class SecurityDepositDtos {
    public record DepositReceiptRequest(@NotNull Long agreementId, @NotNull LocalDate transactionDate, @NotNull @DecimalMin(value = "0.01") BigDecimal amount, @NotBlank String paymentMode, String referenceNumber, String notes) {}
    public record DepositRefundRequest(@NotNull Long agreementId, @NotNull LocalDate transactionDate, @NotNull @DecimalMin(value = "0.01") BigDecimal amount, @NotBlank String paymentMode, String referenceNumber, @NotBlank String reason) {}
    public record DepositAdjustmentRequest(@NotNull Long agreementId, @NotNull Long invoiceId, @NotNull LocalDate transactionDate, @NotNull @DecimalMin(value = "0.01") BigDecimal amount, String notes) {}
    public record DepositReverseRequest(@NotBlank String reason) {}
    public record DepositTransactionResponse(Long id, String depositNumber, Long agreementId, String agreementNumber, Long partyId, String partyName, Long siteId, String siteName, String transactionType, LocalDate transactionDate, BigDecimal amount, String paymentMode, String referenceNumber, Long relatedInvoiceId, String relatedInvoiceNumber, Long sourceDepositTransactionId, String status, String notes, Instant postedAt, String postedBy, Instant reversedAt, String reversedBy, String reversalReason, long version) {}
    public record DepositSummaryResponse(Long agreementId, String agreementNumber, BigDecimal requiredDeposit, BigDecimal received, BigDecimal adjusted, BigDecimal refunded, BigDecimal available, BigDecimal shortfallOrExcess) {}
}
