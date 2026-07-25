package com.stocksync.quotation.dto;

import com.stocksync.quotation.entity.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;

public record QuotationResponse(Long id,String quotationNumber,Long partyId,String partyName,Long siteId,String siteName,
        LocalDate quotationDate,LocalDate validUntil,RentalType rentalType,QuotationStatus status,
        BigDecimal transportCharge,BigDecimal loadingCharge,BigDecimal unloadingCharge,BigDecimal taxRate,
        BigDecimal subtotal,BigDecimal taxAmount,BigDecimal grandTotal,String terms,String notes,
        List<QuotationItemResponse> items,Long version,Instant createdAt,Instant updatedAt) {}
