package com.stocksync.quotation.service;

import com.stocksync.quotation.entity.RentalType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface QuotationAccess {
    ApprovedQuotation requireApprovedForConversion(Long quotationId);
    void markConverted(Long quotationId);

    record ApprovedQuotation(Long id,Long partyId,Long siteId,LocalDate quotationDate,LocalDate validUntil,
            RentalType rentalType,BigDecimal transportCharge,BigDecimal loadingCharge,BigDecimal unloadingCharge,
            String terms,String notes,List<ApprovedItem> items) {}
    record ApprovedItem(Long itemId,BigDecimal quantity,BigDecimal unitRate,BigDecimal rentalRate,String notes) {}
}
