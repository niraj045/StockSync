package com.stocksync.inventory.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
public record AdjustmentRequest(@NotNull LocalDate date,@NotNull Direction direction,@NotBlank @Size(max=500)String reason,
        @Size(max=1000)String notes,@NotEmpty List<@Valid StockLineRequest> items){
    public enum Direction{IN,OUT}
}
