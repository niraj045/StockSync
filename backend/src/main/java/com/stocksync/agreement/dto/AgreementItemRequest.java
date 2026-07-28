package com.stocksync.agreement.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;

public record AgreementItemRequest(
 @NotNull Long itemId,@NotNull @DecimalMin("0.0001") BigDecimal contractedQuantity,
 @NotNull @DecimalMin("0.0001") BigDecimal rate,@NotNull @DecimalMin("0") BigDecimal areaRate,
 @NotNull @DecimalMin("0") BigDecimal weightRate,@NotNull @DecimalMin("0") BigDecimal lossRatePerPiece,
 @NotNull @DecimalMin("0") BigDecimal lossRatePerWeight,@NotNull @DecimalMin("0") BigDecimal damageRate,
 @PositiveOrZero Integer sequence,@Size(max=500) String notes,
 List<@Valid AgreementItemSlabRequest> slabs){}
