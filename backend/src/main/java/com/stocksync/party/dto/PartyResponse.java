package com.stocksync.party.dto;
public record PartyResponse(Long id, String legalName, String tradeName, String gstin, String pan,
        String contactPerson, String phone, String email, String address, String state,
        String notes, boolean active, long version) {}
