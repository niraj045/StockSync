package com.stocksync.migration.controller;

import com.stocksync.migration.dto.*;
import com.stocksync.migration.entity.ImportBatchStatus;
import com.stocksync.migration.entity.ImportLocationType;
import com.stocksync.migration.entity.ImportValidationStatus;
import com.stocksync.migration.service.StockImportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stock-imports")
public class StockImportController {
    private final StockImportService service;

    public StockImportController(StockImportService service) {
        this.service = service;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public StockImportBatchResponse upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String notes,
            HttpServletRequest request) {
        return service.upload(file, notes, request);
    }

    @GetMapping
    public Page<StockImportBatchResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ImportBatchStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {
        return service.list(search, status, pageable);
    }

    @GetMapping("/{id}")
    public StockImportBatchResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/{id}/rows")
    public Page<StockImportRowResponse> rows(
            @PathVariable Long id,
            @RequestParam(required = false) ImportValidationStatus status,
            @RequestParam(required = false) ImportLocationType locationType,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 25, sort = {"sourceExcelRow", "sourceExcelColumn"}) Pageable pageable) {
        return service.rowPage(id, status, locationType, search, pageable);
    }

    @PutMapping("/{id}/rows/{rowId}/item-mapping")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public StockImportBatchResponse mapItem(
            @PathVariable Long id,
            @PathVariable Long rowId,
            @Valid @RequestBody ItemMappingRequest body,
            HttpServletRequest request) {
        return service.mapItem(id, rowId, body, request);
    }

    @GetMapping("/{id}/location-mappings")
    public List<LocationMappingResponse> locationMappings(@PathVariable Long id) {
        return service.locations(id);
    }

    @PutMapping("/{id}/location-mappings")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public List<LocationMappingResponse> mapLocation(
            @PathVariable Long id,
            @Valid @RequestBody LocationMappingRequest body,
            HttpServletRequest request) {
        return service.mapLocation(id, body, request);
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public StockImportPreviewResponse validate(@PathVariable Long id, HttpServletRequest request) {
        return service.validate(id, request);
    }

    @PostMapping("/{id}/auto-map")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public StockImportPreviewResponse autoMap(@PathVariable Long id, HttpServletRequest request) {
        return service.autoMap(id, request);
    }

    @GetMapping("/{id}/preview")
    public StockImportPreviewResponse preview(@PathVariable Long id) {
        return service.preview(id);
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasRole('ADMIN')")
    public StockImportPreviewResponse post(
            @PathVariable Long id,
            @Valid @RequestBody PostImportRequest body,
            HttpServletRequest request) {
        return service.post(id, body, request);
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasRole('ADMIN')")
    public StockImportPreviewResponse reverse(
            @PathVariable Long id,
            @Valid @RequestBody ReverseImportRequest body,
            HttpServletRequest request) {
        return service.reverse(id, body, request);
    }

    @GetMapping("/{id}/report")
    public StockImportReportResponse report(@PathVariable Long id) {
        return service.report(id);
    }
}
