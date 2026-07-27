package com.stocksync.exception.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record SiteTransferRequest(
    Long id,
    
    @NotNull(message = "Source agreement ID is required")
    Long sourceAgreementId,
    
    @NotNull(message = "Destination agreement ID is required")
    Long destinationAgreementId,
    
    @NotNull(message = "Source party ID is required")
    Long sourcePartyId,
    
    @NotNull(message = "Source site ID is required")
    Long sourceSiteId,
    
    @NotNull(message = "Destination party ID is required")
    Long destinationPartyId,
    
    @NotNull(message = "Destination site ID is required")
    Long destinationSiteId,
    
    @NotNull(message = "Transfer date is required")
    LocalDate transferDate,
    
    @Size(max = 50)
    String vehicleNumber,
    
    @Size(max = 100)
    String driverName,
    
    @Size(max = 20)
    String driverPhone,
    
    Long transporterId,
    
    @Size(max = 1000)
    String notes,
    
    @NotEmpty(message = "At least one item line is required")
    @Valid
    List<SiteTransferItemRequest> items
) {}
