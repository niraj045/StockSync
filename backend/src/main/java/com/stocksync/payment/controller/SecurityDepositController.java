package com.stocksync.payment.controller;

import com.stocksync.payment.dto.SecurityDepositDtos.*;
import com.stocksync.payment.service.SecurityDepositService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/security-deposits")
public class SecurityDepositController {
    private final SecurityDepositService service;
    public SecurityDepositController(SecurityDepositService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<Page<DepositTransactionResponse>> list(@RequestParam(required = false) Long agreementId, @RequestParam(required = false) Long partyId,
            @RequestParam(required = false) Long siteId, @RequestParam(required = false) Long invoiceId, @RequestParam(required = false) String status, Pageable pageable) {
        return ResponseEntity.ok(service.list(agreementId, partyId, siteId, invoiceId, status, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepositTransactionResponse> get(@PathVariable Long id) { return ResponseEntity.ok(service.get(id)); }

    @PostMapping("/receipt")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTS')")
    public ResponseEntity<DepositTransactionResponse> receipt(@RequestBody @Valid DepositReceiptRequest r, HttpServletRequest http) { return ResponseEntity.ok(service.receipt(r, http)); }

    @PostMapping("/refund")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTS')")
    public ResponseEntity<DepositTransactionResponse> refund(@RequestBody @Valid DepositRefundRequest r, HttpServletRequest http) { return ResponseEntity.ok(service.refund(r, http)); }

    @PostMapping("/adjust-to-invoice")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTS')")
    public ResponseEntity<DepositTransactionResponse> adjust(@RequestBody @Valid DepositAdjustmentRequest r, HttpServletRequest http) { return ResponseEntity.ok(service.adjust(r, http)); }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepositTransactionResponse> reverse(@PathVariable Long id, @RequestBody @Valid DepositReverseRequest r, HttpServletRequest http) { return ResponseEntity.ok(service.reverse(id, r.reason(), http)); }

    @GetMapping("/agreement/{agreementId}/summary")
    public ResponseEntity<DepositSummaryResponse> summary(@PathVariable Long agreementId) { return ResponseEntity.ok(service.summary(agreementId)); }
}
