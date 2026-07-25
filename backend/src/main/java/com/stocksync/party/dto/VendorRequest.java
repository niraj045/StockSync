package com.stocksync.party.dto;
import jakarta.validation.constraints.*;
public record VendorRequest(
        @NotBlank @Size(max=150) String name,
        @Pattern(regexp="^[0-9A-Z]{15}$", message="GSTIN must contain 15 uppercase letters or digits") String gstin,
        @Size(max=100) String contactPerson, @Size(max=20) String phone,
        @Email @Size(max=150) String email, @Size(max=500) String address,
        @Size(max=1000) String notes, @NotNull Boolean active, Long version) {}
