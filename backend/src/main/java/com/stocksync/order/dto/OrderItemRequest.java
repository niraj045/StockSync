package com.stocksync.order.dto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public record OrderItemRequest(@NotNull Long itemId,@NotNull@DecimalMin("0.0001")BigDecimal orderedQuantity){}
