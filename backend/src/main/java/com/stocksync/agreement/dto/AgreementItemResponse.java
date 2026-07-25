package com.stocksync.agreement.dto;
import java.math.BigDecimal;
public record AgreementItemResponse(Long id,Long itemId,String itemCode,String itemName,String unit,BigDecimal agreedQuantity,
        BigDecimal unitRate,BigDecimal rentalRate,String notes){}
