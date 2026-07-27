package com.stocksync.payment.dto;

import java.math.BigDecimal;

public class OutstandingDtos {
    public record InvoiceOutstandingResponse(Long invoiceId, String invoiceNumber, BigDecimal invoiceTotal, BigDecimal cashAllocated, BigDecimal tdsAllocated, BigDecimal depositAdjusted, BigDecimal outstanding, String paymentStatus) {}
    public record SummaryResponse(BigDecimal totalBilled, BigDecimal cashReceived, BigDecimal tds, BigDecimal depositAdjustments, BigDecimal outstanding, BigDecimal availableAdvance, BigDecimal availableSecurityDeposit) {}
    public record AgreementOutstandingResponse(BigDecimal totalInvoiced, BigDecimal totalSettled, BigDecimal outstanding, BigDecimal requiredDeposit, BigDecimal availableDeposit) {}
}
