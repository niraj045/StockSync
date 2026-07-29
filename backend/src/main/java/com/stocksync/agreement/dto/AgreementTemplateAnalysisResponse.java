package com.stocksync.agreement.dto;

import java.util.List;
import java.util.Map;

public record AgreementTemplateAnalysisResponse(
        Long templateId,
        String templateName,
        String analysisStatus,
        int pageCount,
        String checksumSha256,
        Map<String, String> detectedFields,
        List<String> warnings,
        String extractedText
) {}
