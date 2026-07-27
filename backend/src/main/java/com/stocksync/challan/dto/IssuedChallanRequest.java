package com.stocksync.challan.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record IssuedChallanRequest(
    @NotNull Long siteOrderId,
    @NotNull LocalDate dispatchDate,
    @Size(max = 50) String vehicleNumber,
    @Size(max = 100) String driverName,
    @Size(max = 1000) String notes,
    @NotEmpty List<IssuedChallanItemRequest> items
) {}
