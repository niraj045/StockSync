package com.stocksync.quotation.dto;

import java.time.Instant;

public record QuotationTemplateResponse(
        Long id, String templateCode, String name, String description,
        String companyName, String companyAddress, String companyGstin,
        String headerText, String footerText, String defaultPartATitle, String defaultPartBTitle, String defaultTerms, String defaultNotes,
        Long logoAttachmentId, boolean active, long version,
        Instant createdAt, String createdBy, Instant updatedAt, String updatedBy) {}

