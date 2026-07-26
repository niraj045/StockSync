package com.stocksync.migration.entity;

import com.stocksync.common.persistence.AuditedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="stock_import_batches")
public class StockImportBatch extends AuditedEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="batch_code",nullable=false,length=80) private String batchCode;
    @Column(name="import_type",nullable=false,length=40) private String importType="CLIENT_OPENING_STOCK";
    @Column(name="original_filename",nullable=false) private String originalFilename;
    @Column(name="stored_filename",nullable=false) private String storedFilename;
    @Column(name="storage_path",nullable=false,length=500) private String storagePath;
    @Column(name="file_checksum",nullable=false,length=64) private String fileChecksum;
    @Column(name="source_format",nullable=false,length=50) private String sourceFormat="STEELFAB_STOCK_SNAPSHOT_V1";
    @Column(name="party_snapshot_date",nullable=false) private LocalDate partySnapshotDate;
    @Column(name="godown_snapshot_date",nullable=false) private LocalDate godownSnapshotDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private ImportBatchStatus status;
    @Column(name="total_source_rows",nullable=false) private int totalSourceRows;
    @Column(name="total_balance_rows",nullable=false) private int totalBalanceRows;
    @Column(name="valid_rows",nullable=false) private int validRows;
    @Column(name="warning_rows",nullable=false) private int warningRows;
    @Column(name="error_rows",nullable=false) private int errorRows;
    @Column(name="expected_party_total",nullable=false,precision=19,scale=4) private BigDecimal expectedPartyTotal=BigDecimal.ZERO;
    @Column(name="expected_godown_total",nullable=false,precision=19,scale=4) private BigDecimal expectedGodownTotal=BigDecimal.ZERO;
    @Column(name="imported_at") private Instant importedAt; @Column(name="imported_by",length=50) private String importedBy;
    @Column(name="reversed_at") private Instant reversedAt; @Column(name="reversed_by",length=50) private String reversedBy;
    @Column(name="reversal_reason",length=500) private String reversalReason; @Column(length=1000) private String notes;
    public Long getId(){return id;} public String getBatchCode(){return batchCode;} public void setBatchCode(String v){batchCode=v;}
    public String getImportType(){return importType;} public void setImportType(String v){importType=v;}
    public String getOriginalFilename(){return originalFilename;} public void setOriginalFilename(String v){originalFilename=v;}
    public String getStoredFilename(){return storedFilename;} public void setStoredFilename(String v){storedFilename=v;}
    public String getStoragePath(){return storagePath;} public void setStoragePath(String v){storagePath=v;}
    public String getFileChecksum(){return fileChecksum;} public void setFileChecksum(String v){fileChecksum=v;}
    public String getSourceFormat(){return sourceFormat;} public void setSourceFormat(String v){sourceFormat=v;}
    public LocalDate getPartySnapshotDate(){return partySnapshotDate;} public void setPartySnapshotDate(LocalDate v){partySnapshotDate=v;}
    public LocalDate getGodownSnapshotDate(){return godownSnapshotDate;} public void setGodownSnapshotDate(LocalDate v){godownSnapshotDate=v;}
    public ImportBatchStatus getStatus(){return status;} public void setStatus(ImportBatchStatus v){status=v;}
    public int getTotalSourceRows(){return totalSourceRows;} public void setTotalSourceRows(int v){totalSourceRows=v;}
    public int getTotalBalanceRows(){return totalBalanceRows;} public void setTotalBalanceRows(int v){totalBalanceRows=v;}
    public int getValidRows(){return validRows;} public void setValidRows(int v){validRows=v;}
    public int getWarningRows(){return warningRows;} public void setWarningRows(int v){warningRows=v;}
    public int getErrorRows(){return errorRows;} public void setErrorRows(int v){errorRows=v;}
    public BigDecimal getExpectedPartyTotal(){return expectedPartyTotal;} public void setExpectedPartyTotal(BigDecimal v){expectedPartyTotal=v;}
    public BigDecimal getExpectedGodownTotal(){return expectedGodownTotal;} public void setExpectedGodownTotal(BigDecimal v){expectedGodownTotal=v;}
    public Instant getImportedAt(){return importedAt;} public void setImportedAt(Instant v){importedAt=v;}
    public String getImportedBy(){return importedBy;} public void setImportedBy(String v){importedBy=v;}
    public Instant getReversedAt(){return reversedAt;} public void setReversedAt(Instant v){reversedAt=v;}
    public String getReversedBy(){return reversedBy;} public void setReversedBy(String v){reversedBy=v;}
    public String getReversalReason(){return reversalReason;} public void setReversalReason(String v){reversalReason=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
