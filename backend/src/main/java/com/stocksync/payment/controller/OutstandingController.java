package com.stocksync.payment.controller;

import com.stocksync.payment.dto.OutstandingDtos.*;
import com.stocksync.payment.service.OutstandingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/outstanding")
public class OutstandingController {
    private final OutstandingService service;
    public OutstandingController(OutstandingService service) { this.service = service; }
    @GetMapping("/invoices/{invoiceId}") public ResponseEntity<InvoiceOutstandingResponse> invoice(@PathVariable Long invoiceId) { return ResponseEntity.ok(service.invoice(invoiceId)); }
    @GetMapping("/sites/{siteId}") public ResponseEntity<SummaryResponse> site(@PathVariable Long siteId) { return ResponseEntity.ok(service.site(siteId)); }
    @GetMapping("/parties/{partyId}") public ResponseEntity<SummaryResponse> party(@PathVariable Long partyId) { return ResponseEntity.ok(service.party(partyId)); }
    @GetMapping("/agreements/{agreementId}") public ResponseEntity<AgreementOutstandingResponse> agreement(@PathVariable Long agreementId) { return ResponseEntity.ok(service.agreement(agreementId)); }
}
