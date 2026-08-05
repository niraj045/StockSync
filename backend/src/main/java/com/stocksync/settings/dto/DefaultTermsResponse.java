package com.stocksync.settings.dto;

public record DefaultTermsResponse(
        String documentType,
        String headerText,
        String partATitle,
        String partAText,
        String partBTitle,
        String terms,
        int version
) {}
