package com.stocksync.agreement.dto;
import com.stocksync.agreement.entity.AgreementStatus;
import com.stocksync.quotation.entity.RentalType;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
public record AgreementResponse(Long id,String agreementNumber,Long quotationId,Long templateId,String templateName,Long partyId,String partyName,
        Long siteId,String siteName,LocalDate effectiveDate,LocalDate expiryDate,RentalType rentalType,AgreementStatus status,
        BigDecimal securityDeposit,BigDecimal transportCharge,BigDecimal loadingCharge,BigDecimal unloadingCharge,String terms,String notes,
        boolean generated,String generatedFilename,Instant generatedAt,List<AgreementItemResponse>items,Long version,Instant createdAt,Instant updatedAt){}
