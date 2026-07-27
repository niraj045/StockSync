package com.stocksync.exception.controller;

import com.stocksync.exception.dto.ItemExchangeRequest;
import com.stocksync.exception.dto.ItemExchangeResponse;
import com.stocksync.exception.entity.ItemExchange;
import com.stocksync.exception.service.ItemExchangeService;
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
@RequestMapping("/api/v1/item-exchanges")
public class ItemExchangeController {

    private final ItemExchangeService service;

    public ItemExchangeController(ItemExchangeService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS','VIEWER')")
    public Page<ItemExchangeResponse> list(
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

        Specification<ItemExchange> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String lCase = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("exchangeNumber")), lCase),
                        cb.like(cb.lower(root.get("reason")), lCase)
                ));
            }

            if (documentNumber != null && !documentNumber.isBlank()) {
                predicates.add(cb.equal(root.get("exchangeNumber"), documentNumber.trim()));
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
                predicates.add(cb.or(
                        cb.equal(root.get("expectedItem").get("id"), itemId),
                        cb.equal(root.get("actualItem").get("id"), itemId)
                ));
            }

            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status").as(String.class)), status.trim().toUpperCase()));
            }

            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("exchangeDate"), dateFrom));
            }

            if (dateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("exchangeDate"), dateTo));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return service.list(spec, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS','VIEWER')")
    public ItemExchangeResponse get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public ItemExchangeResponse create(@Valid @RequestBody ItemExchangeRequest body, HttpServletRequest request) {
        return service.create(body, request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public ItemExchangeResponse update(@PathVariable Long id, @Valid @RequestBody ItemExchangeRequest body, HttpServletRequest request) {
        return service.update(id, body, request);
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasRole('ADMIN')")
    public ItemExchangeResponse post(@PathVariable Long id, HttpServletRequest request) {
        return service.post(id, request);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ItemExchangeResponse cancel(@PathVariable Long id, @RequestBody Map<String, String> body, HttpServletRequest request) {
        String reason = body != null ? body.get("cancellationReason") : null;
        return service.cancel(id, reason, request);
    }
}
