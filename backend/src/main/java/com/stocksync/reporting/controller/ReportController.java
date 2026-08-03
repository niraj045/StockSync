package com.stocksync.reporting.controller;

import com.stocksync.audit.service.UserActivityLogService;
import com.stocksync.reporting.dto.ReportDtos.AgeingResponse;
import com.stocksync.reporting.dto.ReportDtos.ExportHistoryResponse;
import com.stocksync.reporting.dto.ReportDtos.ReportCatalogResponse;
import com.stocksync.reporting.dto.ReportDtos.ReportExportRequest;
import com.stocksync.reporting.dto.ReportDtos.ReportExportResponse;
import com.stocksync.reporting.dto.ReportDtos.ReportFilterRequest;
import com.stocksync.reporting.dto.ReportDtos.ReportPreviewResponse;
import com.stocksync.reporting.dto.ReportDtos.SavedFilterRequest;
import com.stocksync.reporting.dto.ReportDtos.SavedFilterResponse;
import com.stocksync.reporting.service.ReportCatalogService;
import com.stocksync.reporting.service.ReportExportService;
import com.stocksync.reporting.service.ReportQueryService;
import com.stocksync.reporting.service.SavedReportFilterService;
import com.stocksync.reporting.service.SbutWorkbookService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
@PreAuthorize("isAuthenticated()")
public class ReportController {
    private final ReportCatalogService catalog;
    private final ReportQueryService queries;
    private final ReportExportService exports;
    private final SavedReportFilterService savedFilters;
    private final UserActivityLogService audit;
    private final SbutWorkbookService sbut;

    public ReportController(ReportCatalogService catalog, ReportQueryService queries, ReportExportService exports,
                            SavedReportFilterService savedFilters, UserActivityLogService audit,SbutWorkbookService sbut) {
        this.catalog = catalog;
        this.queries = queries;
        this.exports = exports;
        this.savedFilters = savedFilters;
        this.audit = audit;
        this.sbut = sbut;
    }

    @GetMapping("/catalog")
    public ReportCatalogResponse catalog(Authentication auth) {
        return new ReportCatalogResponse(catalog.visibleReports(auth));
    }

    @PostMapping("/{reportType}/preview")
    public ReportPreviewResponse preview(@PathVariable String reportType, @RequestBody(required = false) ReportFilterRequest filters,
                                         Authentication auth, HttpServletRequest request) {
        String type = catalog.require(reportType, auth).reportType();
        ReportPreviewResponse response = queries.preview(type, filters);
        audit.log(null, auth.getName(), "REPORT_PREVIEWED", "REPORT", type, "Previewed report " + type, request);
        return response;
    }

    @PostMapping("/{reportType}/export")
    public ReportExportResponse export(@PathVariable String reportType, @Valid @RequestBody ReportExportRequest body,
                                       Authentication auth, HttpServletRequest request) {
        String type = catalog.require(reportType, auth).reportType();
        ReportExportResponse response = exports.export(type, body.filters(), body.format(), auth, request);
        String action = type.startsWith("GST") || type.startsWith("GSTR") ? "GST_REPORT_EXPORTED" : "REPORT_EXPORTED";
        audit.log(null, auth.getName(), action, "REPORT_EXPORT", String.valueOf(response.id()), type + " " + body.format(), request);
        return response;
    }

    @GetMapping("/exports")
    public List<ExportHistoryResponse> exportHistory(Authentication auth) {
        return exports.history(auth);
    }

    @GetMapping("/exports/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication auth) {
        ReportExportService.Download download = exports.download(id, auth);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(download.filename()).build().toString())
                .body(download.resource());
    }

    @GetMapping("/saved-filters")
    public List<SavedFilterResponse> savedFilters(Authentication auth) {
        return savedFilters.list(auth);
    }

    @PostMapping("/saved-filters")
    public SavedFilterResponse createSavedFilter(@Valid @RequestBody SavedFilterRequest body, Authentication auth, HttpServletRequest request) {
        SavedFilterResponse response = savedFilters.create(body, auth);
        audit.log(null, auth.getName(), "SAVED_REPORT_CREATED", "SAVED_REPORT_FILTER", String.valueOf(response.id()), response.reportType(), request);
        return response;
    }

    @PutMapping("/saved-filters/{id}")
    public SavedFilterResponse updateSavedFilter(@PathVariable Long id, @Valid @RequestBody SavedFilterRequest body, Authentication auth, HttpServletRequest request) {
        SavedFilterResponse response = savedFilters.update(id, body, auth);
        audit.log(null, auth.getName(), "SAVED_REPORT_UPDATED", "SAVED_REPORT_FILTER", String.valueOf(response.id()), response.reportType(), request);
        return response;
    }

    @DeleteMapping("/saved-filters/{id}")
    public ResponseEntity<Void> deleteSavedFilter(@PathVariable Long id, Authentication auth, HttpServletRequest request) {
        savedFilters.delete(id, auth);
        audit.log(null, auth.getName(), "SAVED_REPORT_DELETED", "SAVED_REPORT_FILTER", String.valueOf(id), "Deleted saved report filter", request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/outstanding/ageing")
    public AgeingResponse ageing(ReportFilterRequest filters, Authentication auth) {
        catalog.require("OUTSTANDING_AGEING", auth);
        return queries.ageing(filters);
    }

    @GetMapping("/sites/{siteId}/monthly-statement")
    public ReportPreviewResponse monthlyStatement(@PathVariable Long siteId, ReportFilterRequest filters, Authentication auth) {
        catalog.require("MONTHLY_SITE_STATEMENT", auth);
        ReportFilterRequest source = filters == null
                ? new ReportFilterRequest(null, null, null, null, null, null, null, null, null, null, null, 0, 25)
                : filters;
        ReportFilterRequest scoped = new ReportFilterRequest(source.startDate(), source.endDate(), source.partyId(), siteId, source.agreementId(), source.itemId(), source.categoryId(), source.status(), source.documentNumber(), source.user(), source.month(), source.page(), source.size());
        return queries.preview("MONTHLY_SITE_STATEMENT", scoped);
    }

    @GetMapping("/sites/{siteId}/sbut-dr.xlsx")
    public ResponseEntity<byte[]> sbutDeliveryReturn(@PathVariable long siteId) {
        SbutWorkbookService.WorkbookDownload download=sbut.generate(siteId);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(download.filename()).build().toString()).body(download.bytes());
    }

    @GetMapping("/gst/sales-summary")
    public ReportPreviewResponse gstSalesSummary(ReportFilterRequest filters, Authentication auth) {
        catalog.require("GST_TAX_SUMMARY", auth);
        return queries.preview("GST_TAX_SUMMARY", filters);
    }

    @PostMapping("/gst/gstr1-export")
    public ReportExportResponse gstr1(@Valid @RequestBody ReportExportRequest body, Authentication auth, HttpServletRequest request) {
        return export("GSTR1_PREPARATION", body, auth, request);
    }

    @PostMapping("/gst/gstr3b-summary")
    public ReportPreviewResponse gstr3b(@RequestBody(required = false) ReportFilterRequest filters, Authentication auth) {
        catalog.require("GSTR3B_SUMMARY", auth);
        return queries.preview("GSTR3B_SUMMARY", filters);
    }
}
