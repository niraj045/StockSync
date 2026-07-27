package com.stocksync.billing.controller;

import com.stocksync.billing.dto.InvoiceDtos.*;
import com.stocksync.billing.service.InvoiceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> list(
            @RequestParam(required = false) Long agreementId,
            @RequestParam(required = false) Long partyId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(service.list(agreementId, partyId, siteId, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping("/from-billing-run/{billingRunId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTS')")
    public ResponseEntity<InvoiceResponse> createFromBillingRun(@PathVariable Long billingRunId, HttpServletRequest http) {
        return ResponseEntity.ok(service.createFromBillingRun(billingRunId, http));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTS')")
    public ResponseEntity<InvoiceResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid InvoiceUpdateRequest r,
            HttpServletRequest http) {
        return ResponseEntity.ok(service.update(id, r, http));
    }

    @PostMapping("/{id}/generate-pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTS')")
    public ResponseEntity<InvoiceResponse> generatePdf(@PathVariable Long id, HttpServletRequest http) {
        return ResponseEntity.ok(service.generatePdf(id, http));
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTS')")
    public ResponseEntity<InvoiceResponse> issue(@PathVariable Long id, HttpServletRequest http) {
        return ResponseEntity.ok(service.issue(id, http));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTS')")
    public ResponseEntity<InvoiceResponse> cancel(
            @PathVariable Long id,
            @RequestBody @Valid InvoiceCancelRequest r,
            HttpServletRequest http) {
        return ResponseEntity.ok(service.cancel(id, r.reason(), http));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<org.springframework.core.io.Resource> downloadPdf(@PathVariable Long id) {
        InvoiceService.Download d = service.downloadPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + d.filename() + "\"")
                .body(d.resource());
    }
}
