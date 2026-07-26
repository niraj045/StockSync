package com.stocksync.migration.dto;
import jakarta.validation.constraints.*;
public record LocationMappingRequest(@NotBlank @Size(max=5)String sourceExcelColumn,Long partyId,Long siteId,
 boolean createParty,@Size(max=150)String partyName,boolean createSite,@Size(max=150)String siteName,@Size(max=50)String siteCode){}
