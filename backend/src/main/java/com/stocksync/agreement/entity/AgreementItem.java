package com.stocksync.agreement.entity;
import com.stocksync.inventory.entity.Item;
import com.stocksync.quotation.entity.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name="agreement_items")
public class AgreementItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="agreement_id") private Agreement agreement;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="item_id") private Item item;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="source_quotation_item_id") private QuotationItem sourceQuotationItem;
 @Column(name="item_code_snapshot",nullable=false) private String itemCodeSnapshot; @Column(name="item_name_snapshot",nullable=false) private String itemNameSnapshot;
 @Column(name="description_snapshot") private String descriptionSnapshot; @Column(name="size_snapshot") private String sizeSnapshot; @Column(name="unit_snapshot",nullable=false) private String unitSnapshot;
 @Column(name="weight_snapshot",precision=19,scale=4) private BigDecimal weightSnapshot;
 @Column(name="agreed_quantity",nullable=false,precision=19,scale=4) private BigDecimal agreedQuantity;
 @Column(name="unit_rate",nullable=false,precision=19,scale=2) private BigDecimal unitRate;
 @Column(name="rental_rate",nullable=false,precision=19,scale=4) private BigDecimal rentalRate;
 @Enumerated(EnumType.STRING) @Column(name="rental_type",nullable=false) private RentalType rentalType;
 @Column(name="area_rate",nullable=false,precision=19,scale=4) private BigDecimal areaRate=BigDecimal.ZERO;
 @Column(name="weight_rate",nullable=false,precision=19,scale=4) private BigDecimal weightRate=BigDecimal.ZERO;
 @Column(name="loss_rate_per_piece",nullable=false,precision=19,scale=4) private BigDecimal lossRatePerPiece=BigDecimal.ZERO;
 @Column(name="loss_rate_per_weight",nullable=false,precision=19,scale=4) private BigDecimal lossRatePerWeight=BigDecimal.ZERO;
 @Column(name="damage_rate",nullable=false,precision=19,scale=4) private BigDecimal damageRate=BigDecimal.ZERO;
 @Column(name="area",precision=19,scale=4) private BigDecimal area;
 @OneToMany(mappedBy="agreementItem",cascade=CascadeType.ALL,orphanRemoval=true) private java.util.List<AgreementItemSlab> slabs=new java.util.ArrayList<>();
 @Column(name="sequence_number",nullable=false) private int sequence; @Column(length=500) private String notes; @Version private long version;
 public Long getId(){return id;} public Agreement getAgreement(){return agreement;} public void setAgreement(Agreement v){agreement=v;} public Item getItem(){return item;} public void setItem(Item v){item=v;}
 public BigDecimal getArea(){return area;} public void setArea(BigDecimal v){area=v;}
 public java.util.List<AgreementItemSlab> getSlabs(){return slabs;} public void setSlabs(java.util.List<AgreementItemSlab> v){slabs=v;}
 public void replaceSlabs(java.util.List<AgreementItemSlab> v){slabs.clear(); if(v!=null){v.forEach(s->{s.setAgreementItem(this); slabs.add(s);});}}
 public QuotationItem getSourceQuotationItem(){return sourceQuotationItem;} public void setSourceQuotationItem(QuotationItem v){sourceQuotationItem=v;}
 public String getItemCodeSnapshot(){return itemCodeSnapshot;} public void setItemCodeSnapshot(String v){itemCodeSnapshot=v;} public String getItemNameSnapshot(){return itemNameSnapshot;} public void setItemNameSnapshot(String v){itemNameSnapshot=v;}
 public String getDescriptionSnapshot(){return descriptionSnapshot;} public void setDescriptionSnapshot(String v){descriptionSnapshot=v;} public String getSizeSnapshot(){return sizeSnapshot;} public void setSizeSnapshot(String v){sizeSnapshot=v;}
 public String getUnitSnapshot(){return unitSnapshot;} public void setUnitSnapshot(String v){unitSnapshot=v;} public BigDecimal getWeightSnapshot(){return weightSnapshot;} public void setWeightSnapshot(BigDecimal v){weightSnapshot=v;}
 public BigDecimal getAgreedQuantity(){return agreedQuantity;} public void setAgreedQuantity(BigDecimal v){agreedQuantity=v;} public BigDecimal getUnitRate(){return unitRate;} public void setUnitRate(BigDecimal v){unitRate=v;}
 public BigDecimal getRentalRate(){return rentalRate;} public void setRentalRate(BigDecimal v){rentalRate=v;} public RentalType getRentalType(){return rentalType;} public void setRentalType(RentalType v){rentalType=v;}
 public BigDecimal getAreaRate(){return areaRate;} public void setAreaRate(BigDecimal v){areaRate=v;} public BigDecimal getWeightRate(){return weightRate;} public void setWeightRate(BigDecimal v){weightRate=v;}
 public BigDecimal getLossRatePerPiece(){return lossRatePerPiece;} public void setLossRatePerPiece(BigDecimal v){lossRatePerPiece=v;} public BigDecimal getLossRatePerWeight(){return lossRatePerWeight;} public void setLossRatePerWeight(BigDecimal v){lossRatePerWeight=v;}
 public BigDecimal getDamageRate(){return damageRate;} public void setDamageRate(BigDecimal v){damageRate=v;} public int getSequence(){return sequence;} public void setSequence(int v){sequence=v;}
 public String getNotes(){return notes;} public void setNotes(String v){notes=v;} public long getVersion(){return version;}
}
