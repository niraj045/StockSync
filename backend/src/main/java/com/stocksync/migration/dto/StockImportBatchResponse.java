package com.stocksync.migration.dto;
import com.stocksync.migration.entity.ImportBatchStatus;
import java.math.BigDecimal;import java.time.*;
public record StockImportBatchResponse(Long id,String batchCode,String importType,String originalFilename,String fileChecksum,
 String sourceFormat,LocalDate partySnapshotDate,LocalDate godownSnapshotDate,ImportBatchStatus status,int totalSourceRows,
 int totalBalanceRows,int validRows,int warningRows,int errorRows,BigDecimal expectedPartyTotal,BigDecimal expectedGodownTotal,
 BigDecimal expectedCombinedTotal,Instant importedAt,String importedBy,Instant reversedAt,String reversedBy,String reversalReason,
 String notes,long version,Instant createdAt,String createdBy){}
