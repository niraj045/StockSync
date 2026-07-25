package com.stocksync.party.dto;
public record VendorResponse(Long id, String name, String gstin, String contactPerson, String phone,
        String email, String address, String notes, boolean active, long version) {}
