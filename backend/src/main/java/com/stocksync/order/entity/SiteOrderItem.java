package com.stocksync.order.entity;
import com.stocksync.inventory.entity.Item;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="site_order_items")
public class SiteOrderItem{
    @Id@GeneratedValue(strategy=GenerationType.IDENTITY)private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="order_id")private SiteOrder order;
    @ManyToOne(fetch=FetchType.LAZY,optional=false)@JoinColumn(name="item_id")private Item item;
    @Column(name="ordered_quantity",nullable=false,precision=19,scale=4)private BigDecimal orderedQuantity;
    @Column(name="issued_quantity",nullable=false,precision=19,scale=4)private BigDecimal issuedQuantity=BigDecimal.ZERO;
    @Column(name="remaining_quantity",insertable=false,updatable=false,precision=19,scale=4)private BigDecimal remainingQuantity;
    @Version private long version;
    public Long getId(){return id;}public SiteOrder getOrder(){return order;}public void setOrder(SiteOrder v){order=v;}public Item getItem(){return item;}public void setItem(Item v){item=v;}
    public BigDecimal getOrderedQuantity(){return orderedQuantity;}public void setOrderedQuantity(BigDecimal v){orderedQuantity=v;}
    public BigDecimal getIssuedQuantity(){return issuedQuantity;}public void setIssuedQuantity(BigDecimal v){issuedQuantity=v;}
    public BigDecimal getRemainingQuantity(){return remainingQuantity==null?orderedQuantity.subtract(issuedQuantity):remainingQuantity;}public long getVersion(){return version;}
}
