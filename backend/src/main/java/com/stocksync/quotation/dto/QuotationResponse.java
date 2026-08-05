package com.stocksync.quotation.dto;

import com.stocksync.quotation.entity.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public record QuotationResponse(
        Long id,String quotationNumber,Long quotationTemplateId,String quotationTemplateCode,String quotationTemplateName,
        String templateCompanyName,String templateCompanyAddress,String templateCompanyGstin,
        String templateHeaderText,String templateFooterText,
        Long partyId,String partyName,Long siteId,String siteName,LocalDate quotationDate,LocalDate validUntil,
        RentalType rentalType,QuotationStatus status,BigDecimal subtotal,DiscountType discountType,
        BigDecimal discountValue,BigDecimal discountAmount,BigDecimal taxableAmount,
        BigDecimal cgstRate,BigDecimal cgstAmount,BigDecimal sgstRate,BigDecimal sgstAmount,
        BigDecimal igstRate,BigDecimal igstAmount,BigDecimal totalTax,
        BigDecimal transportCharge,BigDecimal loadingCharge,BigDecimal unloadingCharge,BigDecimal otherCharge,
        BigDecimal roundOff,BigDecimal grandTotal,BigDecimal securityDeposit,String headerText,String terms,String notes,
        SteelFabExactHireRequest exactHire,Long exactPdfAttachmentId,String exactPdfTemplateCode,
        Integer exactPdfTemplateVersion,Integer exactPdfCoordinatesVersion,String exactPdfChecksumSha256,
        Instant exactPdfFinalizedAt,String exactPdfFinalizedBy,
        String rejectionReason,Instant sentAt,String sentBy,Instant approvedAt,String approvedBy,
        Instant rejectedAt,String rejectedBy,Instant cancelledAt,String cancelledBy,String cancellationReason,
        List<QuotationItemResponse> items,long version,Instant createdAt,String createdBy,Instant updatedAt,String updatedBy) {}
