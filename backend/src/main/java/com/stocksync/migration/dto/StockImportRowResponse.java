package com.stocksync.migration.dto;
import com.stocksync.migration.entity.*;
import java.math.BigDecimal;import java.time.LocalDate;
public record StockImportRowResponse(Long id,int sourceExcelRow,String sourceExcelColumn,String sourceSrNumber,String sourceItemName,
 String normalizedItemSuggestion,Long mappedItemId,String mappedItemCode,String mappedItemName,String sourceLocationName,
 Long mappedPartyId,String mappedPartyName,Long mappedSiteId,String mappedSiteName,String mappedGodownCode,
 ImportLocationType locationType,BigDecimal quantity,LocalDate snapshotDate,TargetStockBucket targetStockBucket,
 OpeningTransactionType openingTransactionType,ImportValidationStatus validationStatus,String validationMessage,
 boolean duplicateConfirmed,boolean excluded,String exclusionReason,Long postedStockTransactionId,long version){}
