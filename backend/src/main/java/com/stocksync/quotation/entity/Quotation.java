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
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "quotation_template_id")
    private QuotationTemplate quotationTemplate;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "party_id")
    private Party party;
    @Column(name = "party_name_snapshot", nullable = false, length = 150)
    private String partyNameSnapshot;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "site_id")
    private Site site;
    @Column(name = "site_name_snapshot", nullable = false, length = 150)
    private String siteNameSnapshot;
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
    @Enumerated(EnumType.STRING) @Column(name="discount_type",nullable=false,length=20) private DiscountType discountType=DiscountType.NONE;
    @Column(name="discount_value",nullable=false,precision=19,scale=4) private BigDecimal discountValue=BigDecimal.ZERO;
    @Column(name="discount_amount",nullable=false,precision=19,scale=2) private BigDecimal discountAmount=BigDecimal.ZERO;
    @Column(name="taxable_amount",nullable=false,precision=19,scale=2) private BigDecimal taxableAmount=BigDecimal.ZERO;
    @Column(name="cgst_rate",nullable=false,precision=7,scale=4) private BigDecimal cgstRate=BigDecimal.ZERO;
    @Column(name="cgst_amount",nullable=false,precision=19,scale=2) private BigDecimal cgstAmount=BigDecimal.ZERO;
    @Column(name="sgst_rate",nullable=false,precision=7,scale=4) private BigDecimal sgstRate=BigDecimal.ZERO;
    @Column(name="sgst_amount",nullable=false,precision=19,scale=2) private BigDecimal sgstAmount=BigDecimal.ZERO;
    @Column(name="igst_rate",nullable=false,precision=7,scale=4) private BigDecimal igstRate=BigDecimal.ZERO;
    @Column(name="igst_amount",nullable=false,precision=19,scale=2) private BigDecimal igstAmount=BigDecimal.ZERO;
    @Column(name="total_tax",nullable=false,precision=19,scale=2) private BigDecimal totalTax=BigDecimal.ZERO;
    @Column(name="other_charge",nullable=false,precision=19,scale=2) private BigDecimal otherCharge=BigDecimal.ZERO;
    @Column(name="round_off",nullable=false,precision=19,scale=2) private BigDecimal roundOff=BigDecimal.ZERO;
    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    @Column(name = "grand_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;
    @Column(name="security_deposit",nullable=false,precision=19,scale=2) private BigDecimal securityDeposit=BigDecimal.ZERO;
    @Column(length = 4000) private String terms;
    @Column(length = 1000) private String notes;
    @Column(name="rejection_reason",length=1000) private String rejectionReason;
    @Column(name="sent_at") private java.time.Instant sentAt; @Column(name="sent_by",length=50) private String sentBy;
    @Column(name="approved_at") private java.time.Instant approvedAt; @Column(name="approved_by",length=50) private String approvedBy;
    @Column(name="rejected_at") private java.time.Instant rejectedAt; @Column(name="rejected_by",length=50) private String rejectedBy;
    @Column(name="cancelled_at") private java.time.Instant cancelledAt; @Column(name="cancelled_by",length=50) private String cancelledBy;
    @Column(name="cancellation_reason",length=1000) private String cancellationReason;
    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<QuotationItem> items = new ArrayList<>();

    public Long getId(){return id;} public String getQuotationNumber(){return quotationNumber;} public void setQuotationNumber(String v){quotationNumber=v;}
    public QuotationTemplate getQuotationTemplate(){return quotationTemplate;} public void setQuotationTemplate(QuotationTemplate v){quotationTemplate=v;}
    public Party getParty(){return party;} public void setParty(Party v){party=v;}
    public String getPartyNameSnapshot(){return partyNameSnapshot;} public void setPartyNameSnapshot(String v){partyNameSnapshot=v;}
    public Site getSite(){return site;} public void setSite(Site v){site=v;}
    public String getSiteNameSnapshot(){return siteNameSnapshot;} public void setSiteNameSnapshot(String v){siteNameSnapshot=v;}
    public LocalDate getQuotationDate(){return quotationDate;} public void setQuotationDate(LocalDate v){quotationDate=v;}
    public LocalDate getValidUntil(){return validUntil;} public void setValidUntil(LocalDate v){validUntil=v;}
    public RentalType getRentalType(){return rentalType;} public void setRentalType(RentalType v){rentalType=v;}
    public QuotationStatus getStatus(){return status;} public void setStatus(QuotationStatus v){status=v;}
    public BigDecimal getTransportCharge(){return transportCharge;} public void setTransportCharge(BigDecimal v){transportCharge=v;}
    public BigDecimal getLoadingCharge(){return loadingCharge;} public void setLoadingCharge(BigDecimal v){loadingCharge=v;}
    public BigDecimal getUnloadingCharge(){return unloadingCharge;} public void setUnloadingCharge(BigDecimal v){unloadingCharge=v;}
    public BigDecimal getTaxRate(){return taxRate;} public void setTaxRate(BigDecimal v){taxRate=v;}
    public BigDecimal getSubtotal(){return subtotal;} public void setSubtotal(BigDecimal v){subtotal=v;}
    public DiscountType getDiscountType(){return discountType;} public void setDiscountType(DiscountType v){discountType=v;}
    public BigDecimal getDiscountValue(){return discountValue;} public void setDiscountValue(BigDecimal v){discountValue=v;}
    public BigDecimal getDiscountAmount(){return discountAmount;} public void setDiscountAmount(BigDecimal v){discountAmount=v;}
    public BigDecimal getTaxableAmount(){return taxableAmount;} public void setTaxableAmount(BigDecimal v){taxableAmount=v;}
    public BigDecimal getCgstRate(){return cgstRate;} public void setCgstRate(BigDecimal v){cgstRate=v;}
    public BigDecimal getCgstAmount(){return cgstAmount;} public void setCgstAmount(BigDecimal v){cgstAmount=v;}
    public BigDecimal getSgstRate(){return sgstRate;} public void setSgstRate(BigDecimal v){sgstRate=v;}
    public BigDecimal getSgstAmount(){return sgstAmount;} public void setSgstAmount(BigDecimal v){sgstAmount=v;}
    public BigDecimal getIgstRate(){return igstRate;} public void setIgstRate(BigDecimal v){igstRate=v;}
    public BigDecimal getIgstAmount(){return igstAmount;} public void setIgstAmount(BigDecimal v){igstAmount=v;}
    public BigDecimal getTotalTax(){return totalTax;} public void setTotalTax(BigDecimal v){totalTax=v;}
    public BigDecimal getOtherCharge(){return otherCharge;} public void setOtherCharge(BigDecimal v){otherCharge=v;}
    public BigDecimal getRoundOff(){return roundOff;} public void setRoundOff(BigDecimal v){roundOff=v;}
    public BigDecimal getTaxAmount(){return taxAmount;} public void setTaxAmount(BigDecimal v){taxAmount=v;}
    public BigDecimal getGrandTotal(){return grandTotal;} public void setGrandTotal(BigDecimal v){grandTotal=v;}
    public BigDecimal getSecurityDeposit(){return securityDeposit;} public void setSecurityDeposit(BigDecimal v){securityDeposit=v;}
    public String getTerms(){return terms;} public void setTerms(String v){terms=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
    public String getRejectionReason(){return rejectionReason;} public void setRejectionReason(String v){rejectionReason=v;}
    public java.time.Instant getSentAt(){return sentAt;} public void setSentAt(java.time.Instant v){sentAt=v;}
    public String getSentBy(){return sentBy;} public void setSentBy(String v){sentBy=v;}
    public java.time.Instant getApprovedAt(){return approvedAt;} public void setApprovedAt(java.time.Instant v){approvedAt=v;}
    public String getApprovedBy(){return approvedBy;} public void setApprovedBy(String v){approvedBy=v;}
    public java.time.Instant getRejectedAt(){return rejectedAt;} public void setRejectedAt(java.time.Instant v){rejectedAt=v;}
    public String getRejectedBy(){return rejectedBy;} public void setRejectedBy(String v){rejectedBy=v;}
    public java.time.Instant getCancelledAt(){return cancelledAt;} public void setCancelledAt(java.time.Instant v){cancelledAt=v;}
    public String getCancelledBy(){return cancelledBy;} public void setCancelledBy(String v){cancelledBy=v;}
    public String getCancellationReason(){return cancellationReason;} public void setCancellationReason(String v){cancellationReason=v;}
    public List<QuotationItem> getItems(){return items;}
    public void replaceItems(List<QuotationItem> value){items.clear(); value.forEach(this::addItem);}
    public void addItem(QuotationItem item){item.setQuotation(this);items.add(item);}
}
