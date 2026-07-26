package com.stocksync.migration.dto;
import java.math.BigDecimal;import java.time.LocalDate;import java.util.List;
public record StockImportPreviewResponse(Long batchId,String batchCode,String status,LocalDate partySnapshotDate,LocalDate godownSnapshotDate,
 int sourceRows,int balanceRows,long mappedRows,long warningRows,long errorRows,long excludedRows,long postedRows,
 BigDecimal expectedPartyTotal,BigDecimal expectedGodownTotal,BigDecimal expectedCombinedTotal,
 BigDecimal mappedPartyTotal,BigDecimal mappedGodownTotal,BigDecimal mappedCombinedTotal,
 BigDecimal excludedPartyTotal,BigDecimal excludedGodownTotal,BigDecimal excludedTotal,
 BigDecimal errorPartyTotal,BigDecimal errorGodownTotal,BigDecimal errorTotal,
 BigDecimal postedPartyTotal,BigDecimal postedGodownTotal,BigDecimal postedCombinedTotal,
 boolean postable,List<String>warnings){}
