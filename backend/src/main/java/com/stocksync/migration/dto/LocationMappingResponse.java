package com.stocksync.migration.dto;
public record LocationMappingResponse(String sourceExcelColumn,String sourceLocationName,Long partyId,String partyName,
 Long siteId,String siteName,boolean mapped){}
