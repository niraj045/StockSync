package com.stocksync.workflow.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;

public final class ClientWorkflowDtos {
    private ClientWorkflowDtos() {}

    public record InquiryRequest(@NotNull LocalDate inquiryDate,@NotBlank String source,@NotBlank @Size(max=150) String contactName,
        @Size(max=30) String phone,@Email @Size(max=150) String email,Long partyId,Long siteId,@NotBlank @Size(max=2000) String requirement,
        LocalDate followUpDate,@NotBlank String status,Long quotationId,@Size(max=1000) String notes) {}
    public record InquiryResponse(Long id,String inquiryNumber,LocalDate inquiryDate,String source,String contactName,String phone,String email,
        Long partyId,String partyName,Long siteId,String siteName,String requirement,LocalDate followUpDate,String status,Long quotationId,String quotationNumber,
        String notes,String createdBy,Instant createdAt,String updatedBy,Instant updatedAt) {}

    public record OperationRequest(@NotNull LocalDate operationDate,@NotBlank String operationType,@NotBlank String direction,Long partyId,
        @NotNull Long siteId,Long issuedChallanId,Long receivingChallanId,@Size(max=30) String providerType,@Size(max=150) String providerName,
        @Size(max=150) String transporterName,@Size(max=50) String vehicleNumber,@Size(max=100) String driverName,@PositiveOrZero Integer workerCount,
        @DecimalMin("0") BigDecimal quantity,@NotNull @DecimalMin("0") BigDecimal rate,@NotNull @DecimalMin("0") BigDecimal amount,
        @NotNull Boolean chargeToClient,@NotBlank String status,@Size(max=100) String referenceNumber,@Size(max=1000) String notes) {}
    public record OperationResponse(Long id,String operationNumber,LocalDate operationDate,String operationType,String direction,Long partyId,String partyName,
        Long siteId,String siteName,Long issuedChallanId,String issuedChallanNumber,Long receivingChallanId,String receivingChallanNumber,String providerType,
        String providerName,String transporterName,String vehicleNumber,String driverName,Integer workerCount,BigDecimal quantity,BigDecimal rate,BigDecimal amount,
        boolean chargeToClient,String status,String referenceNumber,String notes,String createdBy,Instant createdAt,String updatedBy,Instant updatedAt) {}
}
