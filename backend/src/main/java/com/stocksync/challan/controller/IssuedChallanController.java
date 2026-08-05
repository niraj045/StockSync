package com.stocksync.challan.controller;

import com.stocksync.challan.dto.*;
import com.stocksync.challan.service.IssuedChallanService;
import com.stocksync.challan.service.IssuedChallanPdfService;
import com.stocksync.challan.service.ClientExcelImportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/challans/issued")
public class IssuedChallanController {
    private final IssuedChallanService service;
    private final IssuedChallanPdfService pdf;
    private final ClientExcelImportService importService;

    public IssuedChallanController(IssuedChallanService service, IssuedChallanPdfService pdf, ClientExcelImportService importService) {
        this.service = service;
        this.pdf = pdf;
        this.importService = importService;
    }

    @GetMapping
    public Page<IssuedChallanResponse> list(@RequestParam(required = false) String search, Pageable pageable) {
        return service.list(search, pageable);
    }

    @GetMapping("/{id}")
    public IssuedChallanResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public IssuedChallanResponse create(@Valid @RequestBody IssuedChallanRequest body, HttpServletRequest request) {
        return service.create(body, request);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS','VIEWER')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        var challan = service.get(id);
        var doc = pdf.generate(challan);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.filename() + "\"")
                .body(doc.content());
    }

    @PostMapping(value = "/import-client-excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> importClientExcel(@ModelAttribute @Valid ClientExcelImportRequest request) {
        int imported = importService.importClientExcel(request.siteOrderId(), request.file());
        return ResponseEntity.ok(Map.of("success", true, "importedCount", imported));
    }
}
