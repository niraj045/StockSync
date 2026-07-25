package com.stocksync.inventory.entity;
import jakarta.persistence.*;
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
    @Column(name="source_type",nullable=false,length=30) private String sourceType;
    @Column(name="source_id",nullable=false) private Long sourceId;
    @Column(length=1000) private String notes;
    @Column(name="created_by",nullable=false,length=50) private String createdBy;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @PrePersist void create(){createdAt=Instant.now();}
    public Long getId(){return id;} public Item getItem(){return item;} public void setItem(Item v){item=v;}
    public String getTransactionType(){return transactionType;} public void setTransactionType(String v){transactionType=v;}
    public LocalDate getTransactionDate(){return transactionDate;} public void setTransactionDate(LocalDate v){transactionDate=v;}
    public BigDecimal getQuantity(){return quantity;} public void setQuantity(BigDecimal v){quantity=v;}
    public BigDecimal getWeight(){return weight;} public void setWeight(BigDecimal v){weight=v;}
    public String getDirection(){return direction;} public void setDirection(String v){direction=v;}
    public String getSourceType(){return sourceType;} public void setSourceType(String v){sourceType=v;}
    public Long getSourceId(){return sourceId;} public void setSourceId(Long v){sourceId=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public String getCreatedBy(){return createdBy;} public void setCreatedBy(String v){createdBy=v;}
    public Instant getCreatedAt(){return createdAt;}
}
