package com.stocksync.site.dto;
import com.stocksync.site.entity.SiteStatus;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record SiteRequest(@NotNull Long partyId, @NotBlank @Size(max=150) String siteName,
        @NotBlank @Size(max=50) String siteCode, @Size(max=500) String address,
        @Size(max=100) String contactPerson, LocalDate startDate, LocalDate expectedEndDate,
        @NotNull SiteStatus status, @NotNull Boolean defaulter, LocalDate closedDate,
        @Size(max=1000) String notes, @Size(max=50) String excelTemplateCode, Long version) {}
