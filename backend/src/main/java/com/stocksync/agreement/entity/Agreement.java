package com.stocksync.agreement.entity;

import com.stocksync.common.persistence.AuditedEntity;
import com.stocksync.party.entity.Party;
import com.stocksync.quotation.entity.*;
import com.stocksync.site.entity.Site;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Table(name="agreements")
public class Agreement extends AuditedEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="agreement_number",nullable=false,unique=true,length=50) private String agreementNumber;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="quotation_id") private Quotation quotation;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="template_id") private AgreementTemplate template;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="party_id") private Party party;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="site_id") private Site site;
    @Column(name="effective_date",nullable=false) private LocalDate effectiveDate;
    @Column(name="expiry_date") private LocalDate expiryDate;
    @Enumerated(EnumType.STRING) @Column(name="rental_type",nullable=false,length=40) private RentalType rentalType;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private AgreementStatus status=AgreementStatus.DRAFT;
    @Column(name="security_deposit",nullable=false,precision=19,scale=2) private BigDecimal securityDeposit=BigDecimal.ZERO;
    @Column(name="transport_charge",nullable=false,precision=19,scale=2) private BigDecimal transportCharge=BigDecimal.ZERO;
    @Column(name="loading_charge",nullable=false,precision=19,scale=2) private BigDecimal loadingCharge=BigDecimal.ZERO;
    @Column(name="unloading_charge",nullable=false,precision=19,scale=2) private BigDecimal unloadingCharge=BigDecimal.ZERO;
    @Column(length=4000) private String terms; @Column(length=1000) private String notes;
    @Column(name="generated_filename",length=255) private String generatedFilename;
    @Column(name="generated_storage_path",length=500) private String generatedStoragePath;
    @Column(name="generated_at") private Instant generatedAt;
    @OneToMany(mappedBy="agreement",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("id ASC")
    private List<AgreementItem> items=new ArrayList<>();
    public Long getId(){return id;} public String getAgreementNumber(){return agreementNumber;} public void setAgreementNumber(String v){agreementNumber=v;}
    public Quotation getQuotation(){return quotation;} public void setQuotation(Quotation v){quotation=v;}
    public AgreementTemplate getTemplate(){return template;} public void setTemplate(AgreementTemplate v){template=v;}
    public Party getParty(){return party;} public void setParty(Party v){party=v;} public Site getSite(){return site;} public void setSite(Site v){site=v;}
    public LocalDate getEffectiveDate(){return effectiveDate;} public void setEffectiveDate(LocalDate v){effectiveDate=v;}
    public LocalDate getExpiryDate(){return expiryDate;} public void setExpiryDate(LocalDate v){expiryDate=v;}
    public RentalType getRentalType(){return rentalType;} public void setRentalType(RentalType v){rentalType=v;}
    public AgreementStatus getStatus(){return status;} public void setStatus(AgreementStatus v){status=v;}
    public BigDecimal getSecurityDeposit(){return securityDeposit;} public void setSecurityDeposit(BigDecimal v){securityDeposit=v;}
    public BigDecimal getTransportCharge(){return transportCharge;} public void setTransportCharge(BigDecimal v){transportCharge=v;}
    public BigDecimal getLoadingCharge(){return loadingCharge;} public void setLoadingCharge(BigDecimal v){loadingCharge=v;}
    public BigDecimal getUnloadingCharge(){return unloadingCharge;} public void setUnloadingCharge(BigDecimal v){unloadingCharge=v;}
    public String getTerms(){return terms;} public void setTerms(String v){terms=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public String getGeneratedFilename(){return generatedFilename;} public void setGeneratedFilename(String v){generatedFilename=v;}
    public String getGeneratedStoragePath(){return generatedStoragePath;} public void setGeneratedStoragePath(String v){generatedStoragePath=v;}
    public Instant getGeneratedAt(){return generatedAt;} public void setGeneratedAt(Instant v){generatedAt=v;}
    public List<AgreementItem> getItems(){return items;} public void replaceItems(List<AgreementItem> v){items.clear();v.forEach(this::addItem);}
    public void addItem(AgreementItem i){i.setAgreement(this);items.add(i);}
}
