package com.stocksync.dashboard.controller;

import com.stocksync.dashboard.dto.DashboardDtos.DashboardOverviewResponse;
import com.stocksync.dashboard.service.DashboardOverviewService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
    private final DashboardOverviewService service;

    public DashboardController(DashboardOverviewService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    public DashboardOverviewResponse overview(
            @RequestParam(required = false) Long partyId,
            @RequestParam(required = false) Long siteId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            Authentication auth) {
        return service.overview(partyId, siteId, categoryId, dateFrom, dateTo, auth);
    }
}
