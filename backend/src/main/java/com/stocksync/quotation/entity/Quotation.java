package com.stocksync.quotation.entity;

import com.stocksync.common.persistence.AuditedEntity;
import com.stocksync.party.entity.Party;
import com.stocksync.site.entity.Site;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotations")
public class Quotation extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "quotation_number", nullable = false, unique = true, length = 50)
    private String quotationNumber;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "party_id")
    private Party party;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "site_id")
    private Site site;
    @Column(name = "quotation_date", nullable = false) private LocalDate quotationDate;
    @Column(name = "valid_until", nullable = false) private LocalDate validUntil;
    @Enumerated(EnumType.STRING) @Column(name = "rental_type", nullable = false, length = 40)
    private RentalType rentalType;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private QuotationStatus status = QuotationStatus.DRAFT;
    @Column(name = "transport_charge", nullable = false, precision = 19, scale = 2)
    private BigDecimal transportCharge = BigDecimal.ZERO;
    @Column(name = "loading_charge", nullable = false, precision = 19, scale = 2)
    private BigDecimal loadingCharge = BigDecimal.ZERO;
    @Column(name = "unloading_charge", nullable = false, precision = 19, scale = 2)
    private BigDecimal unloadingCharge = BigDecimal.ZERO;
    @Column(name = "tax_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal taxRate = BigDecimal.ZERO;
    @Column(nullable = false, precision = 19, scale = 2) private BigDecimal subtotal = BigDecimal.ZERO;
    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    @Column(name = "grand_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;
    @Column(length = 4000) private String terms;
    @Column(length = 1000) private String notes;
    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<QuotationItem> items = new ArrayList<>();

    public Long getId(){return id;} public String getQuotationNumber(){return quotationNumber;} public void setQuotationNumber(String v){quotationNumber=v;}
    public Party getParty(){return party;} public void setParty(Party v){party=v;}
    public Site getSite(){return site;} public void setSite(Site v){site=v;}
    public LocalDate getQuotationDate(){return quotationDate;} public void setQuotationDate(LocalDate v){quotationDate=v;}
    public LocalDate getValidUntil(){return validUntil;} public void setValidUntil(LocalDate v){validUntil=v;}
    public RentalType getRentalType(){return rentalType;} public void setRentalType(RentalType v){rentalType=v;}
    public QuotationStatus getStatus(){return status;} public void setStatus(QuotationStatus v){status=v;}
    public BigDecimal getTransportCharge(){return transportCharge;} public void setTransportCharge(BigDecimal v){transportCharge=v;}
    public BigDecimal getLoadingCharge(){return loadingCharge;} public void setLoadingCharge(BigDecimal v){loadingCharge=v;}
    public BigDecimal getUnloadingCharge(){return unloadingCharge;} public void setUnloadingCharge(BigDecimal v){unloadingCharge=v;}
    public BigDecimal getTaxRate(){return taxRate;} public void setTaxRate(BigDecimal v){taxRate=v;}
    public BigDecimal getSubtotal(){return subtotal;} public void setSubtotal(BigDecimal v){subtotal=v;}
    public BigDecimal getTaxAmount(){return taxAmount;} public void setTaxAmount(BigDecimal v){taxAmount=v;}
    public BigDecimal getGrandTotal(){return grandTotal;} public void setGrandTotal(BigDecimal v){grandTotal=v;}
    public String getTerms(){return terms;} public void setTerms(String v){terms=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public List<QuotationItem> getItems(){return items;}
    public void replaceItems(List<QuotationItem> value){items.clear(); value.forEach(this::addItem);}
    public void addItem(QuotationItem item){item.setQuotation(this);items.add(item);}
}
