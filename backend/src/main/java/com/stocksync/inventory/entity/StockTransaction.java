package com.stocksync.inventory.entity;
import jakarta.persistence.*;
import com.stocksync.party.entity.Party;
import com.stocksync.site.entity.Site;
import java.math.BigDecimal;
import java.time.*;

@Entity @Table(name="stock_transactions")
public class StockTransaction {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="item_id") private Item item;
    @Column(name="transaction_type",nullable=false,length=30) private String transactionType;
    @Column(name="transaction_date",nullable=false) private LocalDate transactionDate;
    @Column(nullable=false,precision=19,scale=4) private BigDecimal quantity;
    @Column(precision=19,scale=4) private BigDecimal weight;
    @Column(nullable=false,length=3) private String direction;
    @Column(name="stock_bucket",length=20) private String stockBucket;
    @Column(name="source_type",nullable=false,length=30) private String sourceType;
    @Column(name="source_id",nullable=false) private Long sourceId;
    @Column(name="import_batch_id") private Long importBatchId;
    @Column(name="import_row_id") private Long importRowId;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="site_id") private Site site;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="party_id") private Party party;
    @Column(length=1000) private String notes;
    @Column(name="created_by",nullable=false,length=50) private String createdBy;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="reversal_of_transaction_id") private StockTransaction reversalOfTransaction;
    @PrePersist void create(){createdAt=Instant.now();}
    public Long getId(){return id;} public Item getItem(){return item;} public void setItem(Item v){item=v;}
    public String getTransactionType(){return transactionType;} public void setTransactionType(String v){transactionType=v;}
    public LocalDate getTransactionDate(){return transactionDate;} public void setTransactionDate(LocalDate v){transactionDate=v;}
    public BigDecimal getQuantity(){return quantity;} public void setQuantity(BigDecimal v){quantity=v;}
    public BigDecimal getWeight(){return weight;} public void setWeight(BigDecimal v){weight=v;}
    public String getDirection(){return direction;} public void setDirection(String v){direction=v;}
    public String getStockBucket(){return stockBucket;} public void setStockBucket(String v){stockBucket=v;}
    public String getSourceType(){return sourceType;} public void setSourceType(String v){sourceType=v;}
    public Long getSourceId(){return sourceId;} public void setSourceId(Long v){sourceId=v;}
    public Long getImportBatchId(){return importBatchId;} public void setImportBatchId(Long v){importBatchId=v;}
    public Long getImportRowId(){return importRowId;} public void setImportRowId(Long v){importRowId=v;}
    public Site getSite(){return site;} public void setSite(Site v){site=v;} public Party getParty(){return party;} public void setParty(Party v){party=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
    public Instant getCreatedAt(){return createdAt;}
    public StockTransaction getReversalOfTransaction(){return reversalOfTransaction;} public void setReversalOfTransaction(StockTransaction v){reversalOfTransaction=v;}
}
