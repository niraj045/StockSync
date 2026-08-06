package com.stocksync.party.dto;
import jakarta.validation.constraints.*;
public record PartyRequest(
        @NotBlank @Size(max=150) String legalName, @Size(max=150) String tradeName,
        @Size(max=15) String gstin,
        @Size(max=10) String pan,
        @Size(max=100) String contactPerson, @Size(max=20) String phone,
        @Email @Size(max=150) String email, @Size(max=500) String address,
        @Size(max=100) String state, @Size(max=1000) String notes,
        @NotNull Boolean active, Long version) {}
