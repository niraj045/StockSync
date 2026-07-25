package com.stocksync.inventory.entity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name="stock_balances")
public class StockBalance {
    @Id @Column(name="item_id") private Long itemId;
    @OneToOne(fetch=FetchType.LAZY) @MapsId @JoinColumn(name="item_id") private Item item;
    @Column(name="available_quantity",nullable=false,precision=19,scale=4) private BigDecimal availableQuantity=BigDecimal.ZERO;
    @Column(name="issued_quantity",nullable=false,precision=19,scale=4) private BigDecimal issuedQuantity=BigDecimal.ZERO;
    @Column(name="hired_quantity",nullable=false,precision=19,scale=4) private BigDecimal hiredQuantity=BigDecimal.ZERO;
    @Column(name="lost_quantity",nullable=false,precision=19,scale=4) private BigDecimal lostQuantity=BigDecimal.ZERO;
    @Column(name="scrapped_quantity",nullable=false,precision=19,scale=4) private BigDecimal scrappedQuantity=BigDecimal.ZERO;
    @Column(name="available_weight",nullable=false,precision=19,scale=4) private BigDecimal availableWeight=BigDecimal.ZERO;
    @Version private long version;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    @PrePersist @PreUpdate void touch(){updatedAt=Instant.now();}
    public Long getItemId(){return itemId;} public Item getItem(){return item;} public void setItem(Item v){item=v;itemId=v.getId();}
    public BigDecimal getAvailableQuantity(){return availableQuantity;} public void setAvailableQuantity(BigDecimal v){availableQuantity=v;}
    public BigDecimal getIssuedQuantity(){return issuedQuantity;} public BigDecimal getHiredQuantity(){return hiredQuantity;}
    public BigDecimal getLostQuantity(){return lostQuantity;} public BigDecimal getScrappedQuantity(){return scrappedQuantity;}
    public void setScrappedQuantity(BigDecimal v){scrappedQuantity=v;}
    public BigDecimal getAvailableWeight(){return availableWeight;} public void setAvailableWeight(BigDecimal v){availableWeight=v;}
    public long getVersion(){return version;} public Instant getUpdatedAt(){return updatedAt;}
}
