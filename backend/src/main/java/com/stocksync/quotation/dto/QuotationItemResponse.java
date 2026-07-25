package com.stocksync.quotation.dto;

import java.math.BigDecimal;

public record QuotationItemResponse(Long id,Long itemId,String itemCode,String itemName,String unit,
        BigDecimal quantity,BigDecimal unitRate,BigDecimal rentalRate,BigDecimal lineAmount,String notes) {}
