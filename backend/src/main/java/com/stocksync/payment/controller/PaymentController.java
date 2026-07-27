package com.stocksync.payment.controller;

import com.stocksync.payment.dto.PaymentDtos.*;
import com.stocksync.payment.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService service;
    public PaymentController(PaymentService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<Page<PaymentResponse>> list(@RequestParam(required = false) String search, @RequestParam(required = false) Long partyId,
            @RequestParam(required = false) Long siteId, @RequestParam(required = false) String status, @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) LocalDate dateFrom, @RequestParam(required = false) LocalDate dateTo, Pageable pageable) {
        return ResponseEntity.ok(service.list(search, partyId, siteId, status, paymentMode, dateFrom, dateTo, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> get(@PathVariable Long id) { return ResponseEntity.ok(service.get(id)); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTS','OPERATIONS')")
    public ResponseEntity<PaymentResponse> create(@RequestBody @Valid PaymentRequest r, HttpServletRequest http) { return ResponseEntity.ok(service.create(r, http)); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTS')")
    public ResponseEntity<PaymentResponse> update(@PathVariable Long id, @RequestBody @Valid PaymentRequest r, HttpServletRequest http) { return ResponseEntity.ok(service.update(id, r, http)); }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTS')")
    public ResponseEntity<PaymentResponse> post(@PathVariable Long id, HttpServletRequest http) { return ResponseEntity.ok(service.post(id, http)); }

    @PostMapping("/{id}/allocate")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTS')")
    public ResponseEntity<PaymentResponse> allocate(@PathVariable Long id, @RequestBody @Valid AllocateRequest r, HttpServletRequest http) { return ResponseEntity.ok(service.allocate(id, r, http)); }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PaymentResponse> reverse(@PathVariable Long id, @RequestBody @Valid ReasonRequest r, HttpServletRequest http) { return ResponseEntity.ok(service.reverse(id, r.reason(), http)); }

    @GetMapping("/{id}/receipt")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTS')")
    public ResponseEntity<Resource> receipt(@PathVariable Long id, HttpServletRequest http) {
        PaymentService.Download d = service.generateReceipt(id, http);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType())).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + d.filename() + "\"").body(d.resource());
    }

    @GetMapping("/party/{partyId}/eligible-invoices")
    public ResponseEntity<List<EligibleInvoiceResponse>> eligible(@PathVariable Long partyId) { return ResponseEntity.ok(service.eligibleInvoices(partyId)); }

    @GetMapping("/party/{partyId}/available-advance")
    public ResponseEntity<AdvanceResponse> advance(@PathVariable Long partyId) { return ResponseEntity.ok(service.availableAdvance(partyId)); }

    @PutMapping("/{id}/tds-details")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTS')")
    public ResponseEntity<TdsDetailsResponse> updateTds(@PathVariable Long id, @RequestBody @Valid TdsDetailsRequest r, HttpServletRequest http) { return ResponseEntity.ok(service.updateTds(id, r, http)); }

    @PostMapping("/{id}/tds/verify")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTS')")
    public ResponseEntity<TdsDetailsResponse> verifyTds(@PathVariable Long id, HttpServletRequest http) { return ResponseEntity.ok(service.verifyTds(id, http)); }

    @PostMapping("/{id}/tds/reject")
    @PreAuthorize("hasAnyRole('ADMIN','ACCOUNTS')")
    public ResponseEntity<TdsDetailsResponse> rejectTds(@PathVariable Long id, @RequestBody @Valid TdsRejectRequest r, HttpServletRequest http) { return ResponseEntity.ok(service.rejectTds(id, r.reason(), http)); }
}
