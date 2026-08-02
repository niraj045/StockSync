package com.stocksync.quotation.entity;

import com.stocksync.inventory.entity.Item;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name = "quotation_items")
public class QuotationItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "quotation_id") private Quotation quotation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "item_id") private Item item;
    @Column(name="item_code_snapshot",nullable=false,length=50) private String itemCodeSnapshot;
    @Column(name="item_name_snapshot",nullable=false,length=150) private String itemNameSnapshot;
    @Column(name="description_snapshot",length=500) private String descriptionSnapshot;
    @Column(name="size_snapshot",length=100) private String sizeSnapshot;
    @Column(name="unit_snapshot",nullable=false,length=30) private String unitSnapshot;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal quantity;
    @Column(name = "required_quantity", precision = 19, scale = 4) private BigDecimal requiredQuantity;
    @Column(name = "unit_rate", nullable = false, precision = 19, scale = 2) private BigDecimal unitRate;
    @Column(name = "rental_rate", nullable = false, precision = 19, scale = 4) private BigDecimal rentalRate;
    @Column(name = "hire_months", nullable = false, precision = 9, scale = 2) private BigDecimal hireMonths = BigDecimal.ONE;
    @Column(name = "replacement_rate", precision = 19, scale = 2) private BigDecimal replacementRate;
    @Enumerated(EnumType.STRING) @Column(name="rental_type",nullable=false,length=40) private RentalType rentalType;
    @Column(precision=19,scale=4) private BigDecimal area;
    @Column(precision=19,scale=4) private BigDecimal weight;
    @Column(name = "line_amount", nullable = false, precision = 19, scale = 2) private BigDecimal lineAmount;
    @Column(length = 500) private String notes;
    @Column(name="sequence_number",nullable=false) private int sequence;
    @Version private long version;
    public Long getId(){return id;} public Quotation getQuotation(){return quotation;} public void setQuotation(Quotation v){quotation=v;}
    public Item getItem(){return item;} public void setItem(Item v){item=v;}
    public String getItemCodeSnapshot(){return itemCodeSnapshot;} public void setItemCodeSnapshot(String v){itemCodeSnapshot=v;}
    public String getItemNameSnapshot(){return itemNameSnapshot;} public void setItemNameSnapshot(String v){itemNameSnapshot=v;}
    public String getDescriptionSnapshot(){return descriptionSnapshot;} public void setDescriptionSnapshot(String v){descriptionSnapshot=v;}
    public String getSizeSnapshot(){return sizeSnapshot;} public void setSizeSnapshot(String v){sizeSnapshot=v;}
    public String getUnitSnapshot(){return unitSnapshot;} public void setUnitSnapshot(String v){unitSnapshot=v;}
    public BigDecimal getQuantity(){return quantity;} public void setQuantity(BigDecimal v){quantity=v;}
    public BigDecimal getRequiredQuantity(){return requiredQuantity;} public void setRequiredQuantity(BigDecimal v){requiredQuantity=v;}
    public BigDecimal getUnitRate(){return unitRate;} public void setUnitRate(BigDecimal v){unitRate=v;}
    public BigDecimal getRentalRate(){return rentalRate;} public void setRentalRate(BigDecimal v){rentalRate=v;}
    public BigDecimal getHireMonths(){return hireMonths;} public void setHireMonths(BigDecimal v){hireMonths=v;}
    public BigDecimal getReplacementRate(){return replacementRate;} public void setReplacementRate(BigDecimal v){replacementRate=v;}
    public RentalType getRentalType(){return rentalType;} public void setRentalType(RentalType v){rentalType=v;}
    public BigDecimal getArea(){return area;} public void setArea(BigDecimal v){area=v;}
    public BigDecimal getWeight(){return weight;} public void setWeight(BigDecimal v){weight=v;}
    public BigDecimal getLineAmount(){return lineAmount;} public void setLineAmount(BigDecimal v){lineAmount=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public int getSequence(){return sequence;} public void setSequence(int v){sequence=v;}
    public long getVersion(){return version;}
}
