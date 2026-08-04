package com.stocksync.challan.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record ReceivingChallanRequest(
    Long agreementId,
    @NotNull Long partyId,
    @NotNull Long siteId,
    Long linkedIssuedChallanId,
    @NotNull LocalDate receiveDate,
    @Size(max = 50) String vehicleNumber,
    @Size(max = 100) String driverName,
    @Size(max = 20) String driverPhone,
    @Size(max = 50) String refNo,
    Long transporterId,
    @NotNull String sourceType,
    @Size(max = 1000) String notes,
    @NotEmpty List<ReceivingChallanItemRequest> items
) {}
