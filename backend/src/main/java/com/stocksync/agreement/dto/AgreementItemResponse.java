package com.stocksync.agreement.dto;
import com.stocksync.quotation.entity.RentalType;
import java.math.BigDecimal;
import java.util.List;

public record AgreementItemResponse(
 Long id,Long sourceQuotationItemId,Long itemId,String itemCode,String itemName,String description,String size,String unit,
 BigDecimal weight,BigDecimal contractedQuantity,BigDecimal rate,RentalType rentalType,BigDecimal areaRate,
 BigDecimal weightRate,BigDecimal lossRatePerPiece,BigDecimal lossRatePerWeight,BigDecimal damageRate,int sequence,String notes,
 BigDecimal area,List<AgreementItemSlabResponse> slabs){}
