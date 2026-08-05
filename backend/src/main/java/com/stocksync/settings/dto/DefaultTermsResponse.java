package com.stocksync.settings.dto;

public record DefaultTermsResponse(
        String documentType,
        String terms,
        int version
) {}
