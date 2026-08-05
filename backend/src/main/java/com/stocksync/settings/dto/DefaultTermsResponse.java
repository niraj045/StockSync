package com.stocksync.settings.dto;

public record DefaultTermsResponse(
        String documentType,
        String headerText,
        String terms,
        int version
) {}
