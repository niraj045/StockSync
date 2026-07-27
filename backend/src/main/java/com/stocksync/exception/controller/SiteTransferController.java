package com.stocksync.exception.controller;

import com.stocksync.exception.dto.SiteTransferRequest;
import com.stocksync.exception.dto.SiteTransferResponse;
import com.stocksync.exception.entity.SiteTransfer;
import com.stocksync.exception.service.SiteTransferPdfService;
import com.stocksync.exception.service.SiteTransferService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/site-transfers")
public class SiteTransferController {

    private final SiteTransferService service;
    private final SiteTransferPdfService pdf;

    public SiteTransferController(SiteTransferService service, SiteTransferPdfService pdf) {
        this.service = service;
        this.pdf = pdf;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS','VIEWER')")
    public Page<SiteTransferResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String documentNumber,
            @RequestParam(required = false) Long agreementId,
            @RequestParam(required = false) Long partyId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long sourceSiteId,
            @RequestParam(required = false) Long destinationSiteId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable) {

        Specification<SiteTransfer> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String lCase = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("transferNumber")), lCase),
                        cb.like(cb.lower(root.get("notes")), lCase)
                ));
            }

            if (documentNumber != null && !documentNumber.isBlank()) {
                predicates.add(cb.equal(root.get("transferNumber"), documentNumber.trim()));
            }

            if (agreementId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("sourceAgreement").get("id"), agreementId),
                        cb.equal(root.get("destinationAgreement").get("id"), agreementId)
                ));
            }

            if (partyId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("sourceParty").get("id"), partyId),
                        cb.equal(root.get("destinationParty").get("id"), partyId)
                ));
            }

            if (siteId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("sourceSite").get("id"), siteId),
                        cb.equal(root.get("destinationSite").get("id"), siteId)
                ));
            }

            if (sourceSiteId != null) {
                predicates.add(cb.equal(root.get("sourceSite").get("id"), sourceSiteId));
            }

            if (destinationSiteId != null) {
                predicates.add(cb.equal(root.get("destinationSite").get("id"), destinationSiteId));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status").as(String.class)), status.trim().toUpperCase()));
            }

            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transferDate"), dateFrom));
            }

            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transferDate"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return service.list(spec, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS','VIEWER')")
    public SiteTransferResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public SiteTransferResponse create(@Valid @RequestBody SiteTransferRequest body, HttpServletRequest request) {
        return service.create(body, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public SiteTransferResponse update(@PathVariable Long id, @Valid @RequestBody SiteTransferRequest body, HttpServletRequest request) {
        return service.update(id, body, request);
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public SiteTransferResponse post(@PathVariable Long id, HttpServletRequest request) {
        return service.post(id, request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public SiteTransferResponse cancel(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        String reason = body != null ? body.get("cancellationReason") : null;
        return service.cancel(id, reason, request);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        var transfer = service.getById(id);
        var doc = pdf.generate(transfer);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"site_transfer_" + transfer.transferNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(doc.content());
    }
}
