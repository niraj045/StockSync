package com.stocksync.quotation.dto;

import com.stocksync.quotation.entity.RentalType;
import java.math.BigDecimal;

public record QuotationItemResponse(
        Long id,Long itemId,String itemCodeSnapshot,String itemNameSnapshot,String descriptionSnapshot,
        String sizeSnapshot,String unitSnapshot,BigDecimal quantity,BigDecimal rate,BigDecimal requiredQuantity,
        BigDecimal hireMonths,BigDecimal replacementRate,RentalType rentalType,
        BigDecimal area,BigDecimal weight,BigDecimal amount,int sequence,long version) {}
