package com.stocksync.inventory.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record StockLineRequest(@NotNull Long itemId,@NotNull @Positive BigDecimal quantity,@PositiveOrZero BigDecimal unitRate){}
