package com.stocksync.agreement.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record AgreementItemRequest(@NotNull Long itemId,@NotNull @DecimalMin("0.0001")BigDecimal agreedQuantity,
        @NotNull @DecimalMin("0")BigDecimal unitRate,@NotNull @DecimalMin("0")BigDecimal rentalRate,@Size(max=500)String notes){}
