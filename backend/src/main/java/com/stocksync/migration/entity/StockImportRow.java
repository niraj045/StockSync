package com.stocksync.migration.entity;

import com.stocksync.inventory.entity.*;
import com.stocksync.party.entity.Party;
import com.stocksync.site.entity.Site;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="stock_import_rows")
public class StockImportRow {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="batch_id") private StockImportBatch batch;
    @Column(name="source_excel_row",nullable=false) private int sourceExcelRow;
    @Column(name="source_excel_column",nullable=false,length=5) private String sourceExcelColumn;
    @Column(name="source_sr_number",length=30) private String sourceSrNumber;
    @Column(name="source_item_name",nullable=false) private String sourceItemName;
    @Column(name="normalized_item_suggestion") private String normalizedItemSuggestion;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="mapped_item_id") private Item mappedItem;
    @Column(name="source_location_name",nullable=false) private String sourceLocationName;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="mapped_party_id") private Party mappedParty;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="mapped_site_id") private Site mappedSite;
    @Column(name="mapped_godown_code",length=50) private String mappedGodownCode;
    @Enumerated(EnumType.STRING) @Column(name="location_type",nullable=false,length=20) private ImportLocationType locationType;
    @Column(nullable=false,precision=19,scale=4) private BigDecimal quantity;
    @Column(name="snapshot_date",nullable=false) private LocalDate snapshotDate;
    @Enumerated(EnumType.STRING) @Column(name="target_stock_bucket",nullable=false,length=20) private TargetStockBucket targetStockBucket;
    @Enumerated(EnumType.STRING) @Column(name="opening_transaction_type",nullable=false,length=40) private OpeningTransactionType openingTransactionType;
    @Enumerated(EnumType.STRING) @Column(name="validation_status",nullable=false,length=30) private ImportValidationStatus validationStatus;
    @Column(name="validation_message",length=1000) private String validationMessage;
    @Column(name="duplicate_confirmed",nullable=false) private boolean duplicateConfirmed;
    @Column(nullable=false) private boolean excluded; @Column(name="exclusion_reason",length=500) private String exclusionReason;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="posted_stock_transaction_id") private StockTransaction postedStockTransaction;
    @Version private long version; @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @PrePersist void create(){createdAt=Instant.now();updatedAt=createdAt;} @PreUpdate void update(){updatedAt=Instant.now();}
    public Long getId(){return id;} public StockImportBatch getBatch(){return batch;} public void setBatch(StockImportBatch v){batch=v;}
    public int getSourceExcelRow(){return sourceExcelRow;} public void setSourceExcelRow(int v){sourceExcelRow=v;}
    public String getSourceExcelColumn(){return sourceExcelColumn;} public void setSourceExcelColumn(String v){sourceExcelColumn=v;}
    public String getSourceSrNumber(){return sourceSrNumber;} public void setSourceSrNumber(String v){sourceSrNumber=v;}
    public String getSourceItemName(){return sourceItemName;} public void setSourceItemName(String v){sourceItemName=v;}
    public String getNormalizedItemSuggestion(){return normalizedItemSuggestion;} public void setNormalizedItemSuggestion(String v){normalizedItemSuggestion=v;}
    public Item getMappedItem(){return mappedItem;} public void setMappedItem(Item v){mappedItem=v;}
    public String getSourceLocationName(){return sourceLocationName;} public void setSourceLocationName(String v){sourceLocationName=v;}
    public Party getMappedParty(){return mappedParty;} public void setMappedParty(Party v){mappedParty=v;}
    public Site getMappedSite(){return mappedSite;} public void setMappedSite(Site v){mappedSite=v;}
    public String getMappedGodownCode(){return mappedGodownCode;} public void setMappedGodownCode(String v){mappedGodownCode=v;}
    public ImportLocationType getLocationType(){return locationType;} public void setLocationType(ImportLocationType v){locationType=v;}
    public BigDecimal getQuantity(){return quantity;} public void setQuantity(BigDecimal v){quantity=v;}
    public LocalDate getSnapshotDate(){return snapshotDate;} public void setSnapshotDate(LocalDate v){snapshotDate=v;}
    public TargetStockBucket getTargetStockBucket(){return targetStockBucket;} public void setTargetStockBucket(TargetStockBucket v){targetStockBucket=v;}
    public OpeningTransactionType getOpeningTransactionType(){return openingTransactionType;} public void setOpeningTransactionType(OpeningTransactionType v){openingTransactionType=v;}
    public ImportValidationStatus getValidationStatus(){return validationStatus;} public void setValidationStatus(ImportValidationStatus v){validationStatus=v;}
    public String getValidationMessage(){return validationMessage;} public void setValidationMessage(String v){validationMessage=v;}
    public boolean isDuplicateConfirmed(){return duplicateConfirmed;} public void setDuplicateConfirmed(boolean v){duplicateConfirmed=v;}
    public boolean isExcluded(){return excluded;} public void setExcluded(boolean v){excluded=v;}
    public String getExclusionReason(){return exclusionReason;} public void setExclusionReason(String v){exclusionReason=v;}
    public StockTransaction getPostedStockTransaction(){return postedStockTransaction;} public void setPostedStockTransaction(StockTransaction v){postedStockTransaction=v;}
    public long getVersion(){return version;} public Instant getCreatedAt(){return createdAt;} public Instant getUpdatedAt(){return updatedAt;}
}
