package com.stocksync.exception.controller;

import com.stocksync.exception.dto.StockLossRequest;
import com.stocksync.exception.dto.StockLossResponse;
import com.stocksync.exception.entity.StockLoss;
import com.stocksync.exception.service.StockLossService;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stock-losses")
public class StockLossController {

    private final StockLossService service;

    public StockLossController(StockLossService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS','VIEWER')")
    public Page<StockLossResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String documentNumber,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) Long agreementId,
            @RequestParam(required = false) Long partyId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Pageable pageable) {

        Specification<StockLoss> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String lCase = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("lossNumber")), lCase),
                        cb.like(cb.lower(root.get("reason")), lCase)
                ));
            }

            if (documentNumber != null && !documentNumber.isBlank()) {
                predicates.add(cb.equal(root.get("lossNumber"), documentNumber.trim()));
            }

            if (sourceType != null && !sourceType.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("sourceType").as(String.class)), sourceType.trim().toUpperCase()));
            }

            if (agreementId != null) {
                predicates.add(cb.equal(root.get("agreement").get("id"), agreementId));
            }

            if (partyId != null) {
                predicates.add(cb.equal(root.get("party").get("id"), partyId));
            }

            if (siteId != null) {
                predicates.add(cb.equal(root.get("site").get("id"), siteId));
            }

            if (itemId != null) {
                predicates.add(cb.equal(root.get("item").get("id"), itemId));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status").as(String.class)), status.trim().toUpperCase()));
            }

            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("lossDate"), dateFrom));
            }

            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("lossDate"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return service.list(spec, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS','VIEWER')")
    public StockLossResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public StockLossResponse create(@Valid @RequestBody StockLossRequest body, HttpServletRequest request) {
        return service.create(body, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public StockLossResponse update(@PathVariable Long id, @Valid @RequestBody StockLossRequest body, HttpServletRequest request) {
        return service.update(id, body, request);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public StockLossResponse approve(@PathVariable Long id, HttpServletRequest request) {
        return service.approve(id, request);
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasRole('ADMIN')")
    public StockLossResponse reverse(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        String reason = body != null ? body.get("reversalReason") : null;
        return service.reverse(id, reason, request);
    }
}
