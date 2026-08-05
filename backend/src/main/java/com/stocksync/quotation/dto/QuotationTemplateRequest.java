package com.stocksync.quotation.dto;

import jakarta.validation.constraints.*;

public record QuotationTemplateRequest(
        @NotBlank @Size(max=50) String templateCode,
        @NotBlank @Size(max=150) String name,
        @Size(max=500) String description,
        @Size(max=200) String companyName,
        @Size(max=1000) String companyAddress,
        @Pattern(regexp="^$|^[0-9A-Z]{15}$", message="must be a valid 15-character GSTIN") String companyGstin,
        @Size(max=2000) String headerText,
        @Size(max=2000) String footerText,
        @Size(max=255) String defaultPartATitle,
        String defaultPartAText,
        @Size(max=255) String defaultPartBTitle,
        @Size(max=4000) String defaultTerms,
        @Size(max=2000) String defaultNotes,
        Long logoAttachmentId,
        Long version) {}

