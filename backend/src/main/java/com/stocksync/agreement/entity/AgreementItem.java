package com.stocksync.agreement.entity;

import com.stocksync.inventory.entity.Item;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name="agreement_items")
public class AgreementItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="agreement_id") private Agreement agreement;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="item_id") private Item item;
    @Column(name="agreed_quantity",nullable=false,precision=19,scale=4) private BigDecimal agreedQuantity;
    @Column(name="unit_rate",nullable=false,precision=19,scale=2) private BigDecimal unitRate;
    @Column(name="rental_rate",nullable=false,precision=19,scale=4) private BigDecimal rentalRate;
    @Column(length=500) private String notes;
    public Long getId(){return id;} public Agreement getAgreement(){return agreement;} public void setAgreement(Agreement v){agreement=v;}
    public Item getItem(){return item;} public void setItem(Item v){item=v;}
    public BigDecimal getAgreedQuantity(){return agreedQuantity;} public void setAgreedQuantity(BigDecimal v){agreedQuantity=v;}
    public BigDecimal getUnitRate(){return unitRate;} public void setUnitRate(BigDecimal v){unitRate=v;}
    public BigDecimal getRentalRate(){return rentalRate;} public void setRentalRate(BigDecimal v){rentalRate=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
