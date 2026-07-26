package com.stocksync.inventory.entity;

import com.stocksync.common.persistence.AuditedEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "items")
public class Item extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;
    @Column(name = "item_name", nullable = false, length = 150)
    private String itemName;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ItemCategory category;
    private String size;
    @Column(nullable = false, length = 30)
    private String unit;
    @Column(name = "weight_per_piece", precision = 19, scale = 4)
    private BigDecimal weightPerPiece;
    @Column(name = "purchase_value", precision = 19, scale = 2)
    private BigDecimal purchaseValue;
    @Column(name = "rental_configuration", length = 500)
    private String rentalConfiguration;
    @Column(name = "loss_rate", precision = 19, scale = 2)
    private BigDecimal lossRate;
    @Column(name = "scrap_value", precision = 19, scale = 2)
    private BigDecimal scrapValue;
    @Column(name = "minimum_stock", precision = 19, scale = 4)
    private BigDecimal minimumStock;
    @Column(nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public String getItemCode() { return itemCode; }
    public void setItemCode(String itemCode) { this.itemCode = itemCode; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }
    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getWeightPerPiece() { return weightPerPiece; }
    public void setWeightPerPiece(BigDecimal value) { this.weightPerPiece = value; }
    public BigDecimal getPurchaseValue() { return purchaseValue; }
    public void setPurchaseValue(BigDecimal value) { this.purchaseValue = value; }
    public String getRentalConfiguration() { return rentalConfiguration; }
    public void setRentalConfiguration(String value) { this.rentalConfiguration = value; }
    public BigDecimal getLossRate() { return lossRate; }
    public void setLossRate(BigDecimal value) { this.lossRate = value; }
    public BigDecimal getScrapValue() { return scrapValue; }
    public void setScrapValue(BigDecimal value) { this.scrapValue = value; }
    public BigDecimal getMinimumStock() { return minimumStock; }
    public void setMinimumStock(BigDecimal value) { this.minimumStock = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
