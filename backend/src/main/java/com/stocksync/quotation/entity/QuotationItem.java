package com.stocksync.quotation.entity;

import com.stocksync.inventory.entity.Item;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "quotation_items")
public class QuotationItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "quotation_id") private Quotation quotation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "item_id") private Item item;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal quantity;
    @Column(name = "unit_rate", nullable = false, precision = 19, scale = 2) private BigDecimal unitRate;
    @Column(name = "rental_rate", nullable = false, precision = 19, scale = 4) private BigDecimal rentalRate;
    @Column(name = "line_amount", nullable = false, precision = 19, scale = 2) private BigDecimal lineAmount;
    @Column(length = 500) private String notes;
    public Long getId(){return id;} public Quotation getQuotation(){return quotation;} public void setQuotation(Quotation v){quotation=v;}
    public Item getItem(){return item;} public void setItem(Item v){item=v;}
    public BigDecimal getQuantity(){return quantity;} public void setQuantity(BigDecimal v){quantity=v;}
    public BigDecimal getUnitRate(){return unitRate;} public void setUnitRate(BigDecimal v){unitRate=v;}
    public BigDecimal getRentalRate(){return rentalRate;} public void setRentalRate(BigDecimal v){rentalRate=v;}
    public BigDecimal getLineAmount(){return lineAmount;} public void setLineAmount(BigDecimal v){lineAmount=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
