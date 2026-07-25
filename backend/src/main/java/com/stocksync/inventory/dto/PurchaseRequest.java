package com.stocksync.inventory.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
public record PurchaseRequest(@NotNull Long vendorId,@NotNull LocalDate purchaseDate,@Size(max=1000)String notes,
        @NotEmpty List<@Valid StockLineRequest> items){}
