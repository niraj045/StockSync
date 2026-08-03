package com.stocksync.challan.dto;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;

public record IssuedChallanResponse(
    Long id,
    String challanNumber,
    Long siteOrderId,
    String siteOrderNumber,
    Long siteId,
    String siteName,
    Long partyId,
    String partyName,
    String partyAddress,
    String partyGstin,
    String siteAddress,
    String siteContact,
    LocalDate dispatchDate,
    String vehicleNumber,
    String driverName,
    String notes,
    String createdBy,
    Instant createdAt,
    List<IssuedChallanItemResponse> items
) {}
