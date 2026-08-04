package com.stocksync.challan.entity;

import jakarta.persistence.*;
import com.stocksync.inventory.entity.Item;
import java.math.BigDecimal;

@Entity
@Table(name = "issued_challan_items")
public class IssuedChallanItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issued_challan_id")
    private IssuedChallan issuedChallan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(name = "item_code_snapshot", nullable = false, length = 50)
    private String itemCodeSnapshot;

    @Column(name = "item_name_snapshot", nullable = false)
    private String itemNameSnapshot;

    @Column(name = "unit_snapshot", nullable = false, length = 20)
    private String unitSnapshot;

    @Column(length = 255)
    private String notes;

    public Long getId() { return id; }
    public IssuedChallan getIssuedChallan() { return issuedChallan; }
    public void setIssuedChallan(IssuedChallan v) { issuedChallan = v; }
    public Item getItem() { return item; }
    public void setItem(Item v) { item = v; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal v) { quantity = v; }
    public String getItemCodeSnapshot() { return itemCodeSnapshot; }
    public void setItemCodeSnapshot(String v) { itemCodeSnapshot = v; }
    public String getItemNameSnapshot() { return itemNameSnapshot; }
    public void setItemNameSnapshot(String v) { itemNameSnapshot = v; }
    public String getUnitSnapshot() { return unitSnapshot; }
    public void setUnitSnapshot(String v) { unitSnapshot = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
}
