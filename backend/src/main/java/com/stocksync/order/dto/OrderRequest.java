package com.stocksync.order.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
public record OrderRequest(@NotNull Long agreementId,@NotNull LocalDate orderDate,@Size(max=1000)String notes,
        @NotEmpty List<@Valid OrderItemRequest>items,Long version){}
