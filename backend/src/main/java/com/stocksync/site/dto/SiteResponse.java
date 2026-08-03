package com.stocksync.site.dto;
import com.stocksync.site.entity.SiteStatus;
import java.time.LocalDate;
public record SiteResponse(Long id, Long partyId, String partyName, String siteName, String siteCode,
        String address, String contactPerson, LocalDate startDate, LocalDate expectedEndDate,
        SiteStatus status, boolean defaulter, LocalDate closedDate, String notes, String excelTemplateCode, long version) {}
