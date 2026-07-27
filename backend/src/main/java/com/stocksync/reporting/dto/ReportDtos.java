package com.stocksync.reporting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public final class ReportDtos {
    private ReportDtos() {}

    public record ReportDefinitionResponse(
            String reportType,
            String name,
            String category,
            List<String> formats,
            List<String> roles,
            boolean gstPreparation,
            String description
    ) {}

    public record ReportCatalogResponse(List<ReportDefinitionResponse> reports) {}

    public record ReportFilterRequest(
            LocalDate startDate,
            LocalDate endDate,
            Long partyId,
            Long siteId,
            Long agreementId,
            Long itemId,
            Long categoryId,
            String status,
            String documentNumber,
            String user,
            YearMonth month,
            Integer page,
            Integer size
    ) {}

    public record ReportPreviewResponse(
            String reportType,
            List<String> columns,
            List<Map<String, Object>> rows,
            Map<String, BigDecimal> totals,
            int page,
            int size,
            long totalElements,
            String warning
    ) {}

    public record ReportExportRequest(@NotNull ReportFilterRequest filters, @NotNull ExportFormat format) {}

    public enum ExportFormat { PDF, EXCEL, CSV }

    public record ReportExportResponse(
            Long id,
            String reportType,
            ExportFormat format,
            String filename,
            String status,
            Instant generatedAt
    ) {}

    public record SavedFilterRequest(
            @NotBlank String name,
            @NotBlank String reportType,
            @NotNull ReportFilterRequest filters,
            boolean shared
    ) {}

    public record SavedFilterResponse(
            Long id,
            String name,
            String reportType,
            ReportFilterRequest filters,
            String ownerUsername,
            boolean shared,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ExportHistoryResponse(
            Long id,
            String reportType,
            ExportFormat format,
            String filename,
            String status,
            String errorMessage,
            String generatedBy,
            Instant generatedAt,
            Long fileSize
    ) {}

    public record AgeingResponse(
            BigDecimal current,
            BigDecimal days1To30,
            BigDecimal days31To60,
            BigDecimal days61To90,
            BigDecimal moreThan90,
            BigDecimal total
    ) {}
}
