package com.stocksync.agreement.dto;
import com.stocksync.agreement.entity.*;
import com.stocksync.quotation.entity.RentalType;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public record AgreementResponse(
 Long id,String agreementNumber,Long sourceQuotationId,String sourceQuotationNumber,LocalDate sourceQuotationDate,
 Long templateId,String templateCode,String templateName,String templateLayoutKey,Integer templateVersion,
 Long partyId,String partyName,String partyTradeName,String partyGstin,String partyPan,String partyAddress,String partyState,String partyContact,
 Long siteId,String siteName,String siteCode,String siteAddress,String siteContact,
 LocalDate agreementDate,LocalDate effectiveDate,LocalDate expiryDate,RentalType rentalType,BillingCycle billingCycle,
 MeasurementBasis measurementBasis,BillingCommencementRule billingCommencementRule,LocalDate fixedBillingStartDate,
 LocalDate nextBillingDate,LocalDate lastAutoPeriodEnd,
 Integer customBillingCycleDays,int gracePeriodDays,int minimumBillingDays,AgreementStatus status,
 BigDecimal securityDeposit,BigDecimal subtotal,BigDecimal discountAmount,BigDecimal taxableAmount,
 BigDecimal cgstAmount,BigDecimal sgstAmount,BigDecimal igstAmount,BigDecimal totalTax,
 BigDecimal transportCharge,BigDecimal loadingCharge,BigDecimal unloadingCharge,BigDecimal otherCharge,
 BigDecimal roundOff,BigDecimal grandTotal,String headerText,String terms,String notes,
 Long generatedDocumentAttachmentId,String generatedFilename,Instant generatedAt,
 Long signedDocumentAttachmentId,String signedFilename,Instant signedUploadedAt,
 Instant readyForReviewAt,String readyForReviewBy,
 Instant activatedAt,String activatedBy,String terminationReason,Instant terminatedAt,String terminatedBy,
 String cancellationReason,Instant cancelledAt,String cancelledBy,List<AgreementItemResponse> items,
 String billingStartRule,String billingEndRule,
 long version,Instant createdAt,String createdBy,Instant updatedAt,String updatedBy){}
