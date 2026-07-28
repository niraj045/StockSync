package com.stocksync.billing.controller;

import com.stocksync.billing.dto.BillingRunDtos.*;
import com.stocksync.billing.service.BillingRunService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/billing-runs")
public class BillingRunController {

    private final BillingRunService service;

    public BillingRunController(BillingRunService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<Page<BillingRunResponse>> list(
            @RequestParam(required = false) Long agreementId,
            @RequestParam(required = false) Long partyId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) String status,
            Pageable pageable) {
        return ResponseEntity.ok(service.list(agreementId, partyId, siteId, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BillingRunResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(service.get(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTS', 'OPERATIONS')")
    public ResponseEntity<BillingRunResponse> create(@RequestBody @Valid BillingRunRequest r, HttpServletRequest http) {
        return ResponseEntity.ok(service.create(r, http));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTS')")
    public ResponseEntity<BillingRunResponse> update(
            @PathVariable Long id,
            @RequestBody @Valid BillingRunUpdateValuesRequest r,
            HttpServletRequest http) {
        return ResponseEntity.ok(service.update(id, r, http));
    }

    @PostMapping("/{id}/calculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTS', 'OPERATIONS')")
    public ResponseEntity<BillingRunResponse> calculate(@PathVariable Long id, HttpServletRequest http) {
        return ResponseEntity.ok(service.calculate(id, http));
    }

    @PostMapping("/{id}/finalize")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTS')")
    public ResponseEntity<BillingRunResponse> finalizeRun(@PathVariable Long id, HttpServletRequest http) {
        return ResponseEntity.ok(service.finalizeRun(id, http));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTS')")
    public ResponseEntity<BillingRunResponse> cancel(
            @PathVariable Long id,
            @RequestParam String reason,
            HttpServletRequest http) {
        return ResponseEntity.ok(service.cancel(id, reason, http));
    }

    @GetMapping("/eligible-agreements")
    public ResponseEntity<List<EligibleAgreementResponse>> getEligibleAgreements() {
        return ResponseEntity.ok(service.getEligibleAgreements());
    }

    @GetMapping("/agreement/{agreementId}/suggested-period")
    public ResponseEntity<Map<String, LocalDate>> getSuggestedPeriod(@PathVariable Long agreementId) {
        return ResponseEntity.ok(service.getSuggestedPeriod(agreementId));
    }
}
