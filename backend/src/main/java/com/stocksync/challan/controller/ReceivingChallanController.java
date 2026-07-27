package com.stocksync.challan.controller;

import com.stocksync.challan.dto.*;
import com.stocksync.challan.service.ReceivingChallanPdfService;
import com.stocksync.challan.service.ReceivingChallanService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/challans/receiving")
public class ReceivingChallanController {

    private final ReceivingChallanService service;
    private final ReceivingChallanPdfService pdf;

    public ReceivingChallanController(ReceivingChallanService service, ReceivingChallanPdfService pdf) {
        this.service = service;
        this.pdf = pdf;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS','VIEWER')")
    public Page<ReceivingChallanResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String receivingChallanNumber,
            @RequestParam(required = false) Long partyId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long agreementId,
            @RequestParam(required = false) Long linkedIssuedChallanId,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receiveDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate receiveDateTo,
            Pageable pageable) {
        return service.list(
                search,
                receivingChallanNumber,
                partyId,
                siteId,
                agreementId,
                linkedIssuedChallanId,
                sourceType,
                status,
                receiveDateFrom,
                receiveDateTo,
                pageable
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS','VIEWER')")
    public ReceivingChallanResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public ReceivingChallanResponse create(@Valid @RequestBody ReceivingChallanRequest body, HttpServletRequest request) {
        return service.create(body, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public ReceivingChallanResponse update(@PathVariable Long id, @Valid @RequestBody ReceivingChallanRequest body, HttpServletRequest request) {
        return service.update(id, body, request);
    }

    @PostMapping("/{id}/approve-extra")
    @PreAuthorize("hasRole('ADMIN')")
    public ReceivingChallanResponse approveExtra(@PathVariable Long id, HttpServletRequest request) {
        return service.approveExtra(id, request);
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public ReceivingChallanResponse post(@PathVariable Long id, HttpServletRequest request) {
        return service.post(id, request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ReceivingChallanResponse cancel(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        String reason = body != null ? body.get("cancellationReason") : null;
        if (reason == null || reason.isBlank()) {
            reason = "No cancellation reason provided";
        }
        return service.cancel(id, reason, request);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS','VIEWER')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        var challan = service.get(id);
        var doc = pdf.generate(challan);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"receiving_challan_" + challan.receivingChallanNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(doc.content());
    }

    // Lookup endpoints
    @GetMapping("/site-pending-balances")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS','VIEWER')")
    public List<SiteStockBalanceResponse> getSitePendingBalances(@RequestParam Long siteId) {
        return service.getSitePendingBalances(siteId);
    }

    @GetMapping("/issued-lookup")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS','VIEWER')")
    public List<IssuedChallanResponse> getIssuedChallansForSite(@RequestParam Long siteId) {
        return service.getIssuedChallansForSite(siteId);
    }
}
