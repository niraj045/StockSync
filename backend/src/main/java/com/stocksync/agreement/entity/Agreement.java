package com.stocksync.agreement.entity;

import com.stocksync.common.persistence.AuditedEntity;
import com.stocksync.file.entity.FileAttachment;
import com.stocksync.party.entity.Party;
import com.stocksync.quotation.entity.Quotation;
import com.stocksync.quotation.entity.RentalType;
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
 @Column(name="agreement_date",nullable=false) private LocalDate agreementDate;
 @Column(name="effective_date",nullable=false) private LocalDate effectiveDate;
 @Column(name="expiry_date") private LocalDate expiryDate;
 @Enumerated(EnumType.STRING) @Column(name="rental_type",nullable=false,length=40) private RentalType rentalType;
 @Enumerated(EnumType.STRING) @Column(name="billing_cycle",nullable=false,length=20) private BillingCycle billingCycle=BillingCycle.MONTHLY;
 @Enumerated(EnumType.STRING) @Column(name="measurement_basis",nullable=false,length=20) private MeasurementBasis measurementBasis=MeasurementBasis.ITEM_QUANTITY;
 @Enumerated(EnumType.STRING) @Column(name="billing_commencement_rule",nullable=false,length=30) private BillingCommencementRule billingCommencementRule=BillingCommencementRule.FIRST_DISPATCH;
 @Column(name="fixed_billing_start_date") private LocalDate fixedBillingStartDate;
 @Column(name="next_billing_date") private LocalDate nextBillingDate;
 @Column(name="last_auto_period_end") private LocalDate lastAutoPeriodEnd;
 @Column(name="custom_billing_cycle_days") private Integer customBillingCycleDays;
 @Column(name="grace_period_days",nullable=false) private int gracePeriodDays;
 @Column(name="minimum_billing_days",nullable=false) private int minimumBillingDays;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private AgreementStatus status=AgreementStatus.DRAFT;
 @Column(name="party_legal_name_snapshot",nullable=false) private String partyLegalNameSnapshot;
 @Column(name="party_trade_name_snapshot") private String partyTradeNameSnapshot;
 @Column(name="party_gstin_snapshot") private String partyGstinSnapshot;
 @Column(name="party_pan_snapshot") private String partyPanSnapshot;
 @Column(name="party_address_snapshot") private String partyAddressSnapshot;
 @Column(name="party_state_snapshot") private String partyStateSnapshot;
 @Column(name="party_contact_snapshot") private String partyContactSnapshot;
 @Column(name="site_name_snapshot",nullable=false) private String siteNameSnapshot;
 @Column(name="site_code_snapshot",nullable=false) private String siteCodeSnapshot;
 @Column(name="site_address_snapshot") private String siteAddressSnapshot;
 @Column(name="site_contact_snapshot") private String siteContactSnapshot;
 @Column(name="quotation_number_snapshot") private String quotationNumberSnapshot;
 @Column(name="quotation_date_snapshot") private LocalDate quotationDateSnapshot;
 @Column(name="quotation_approved_at_snapshot") private Instant quotationApprovedAtSnapshot;
 @Column(name="security_deposit",precision=19,scale=2,nullable=false) private BigDecimal securityDeposit=BigDecimal.ZERO;
 @Column(precision=19,scale=2,nullable=false) private BigDecimal subtotal=BigDecimal.ZERO;
 @Column(name="discount_amount",precision=19,scale=2,nullable=false) private BigDecimal discountAmount=BigDecimal.ZERO;
 @Column(name="taxable_amount",precision=19,scale=2,nullable=false) private BigDecimal taxableAmount=BigDecimal.ZERO;
 @Column(name="cgst_amount",precision=19,scale=2,nullable=false) private BigDecimal cgstAmount=BigDecimal.ZERO;
 @Column(name="sgst_amount",precision=19,scale=2,nullable=false) private BigDecimal sgstAmount=BigDecimal.ZERO;
 @Column(name="igst_amount",precision=19,scale=2,nullable=false) private BigDecimal igstAmount=BigDecimal.ZERO;
 @Column(name="total_tax",precision=19,scale=2,nullable=false) private BigDecimal totalTax=BigDecimal.ZERO;
 @Column(name="transport_charge",precision=19,scale=2,nullable=false) private BigDecimal transportCharge=BigDecimal.ZERO;
 @Column(name="loading_charge",precision=19,scale=2,nullable=false) private BigDecimal loadingCharge=BigDecimal.ZERO;
 @Column(name="unloading_charge",precision=19,scale=2,nullable=false) private BigDecimal unloadingCharge=BigDecimal.ZERO;
 @Column(name="other_charge",precision=19,scale=2,nullable=false) private BigDecimal otherCharge=BigDecimal.ZERO;
 @Column(name="round_off",precision=19,scale=2,nullable=false) private BigDecimal roundOff=BigDecimal.ZERO;
 @Column(name="grand_total",precision=19,scale=2,nullable=false) private BigDecimal grandTotal=BigDecimal.ZERO;
 @Column(name="header_text",length=2000) private String headerText;
 @Column(length=4000) private String terms; @Column(length=1000) private String notes;
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="generated_document_attachment_id") private FileAttachment generatedDocument;
 @Column(name="generated_filename") private String generatedFilename; @Column(name="generated_storage_path") private String generatedStoragePath;
 @Column(name="generated_at") private Instant generatedAt; 
 @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="signed_document_attachment_id") private FileAttachment signedDocument;
 @Column(name="signed_filename") private String signedFilename; 
 @Column(name="signed_uploaded_at") private Instant signedUploadedAt; 
 @Column(name="ready_for_review_at") private Instant readyForReviewAt;
 @Column(name="ready_for_review_by") private String readyForReviewBy; @Column(name="activated_at") private Instant activatedAt;
 @Column(name="activated_by") private String activatedBy; @Column(name="expired_at") private Instant expiredAt; @Column(name="expired_by") private String expiredBy;
 @Column(name="termination_reason") private String terminationReason; @Column(name="terminated_at") private Instant terminatedAt; @Column(name="terminated_by") private String terminatedBy;
 @Column(name="closed_at") private Instant closedAt; @Column(name="closed_by") private String closedBy;
 @Column(name="cancellation_reason") private String cancellationReason; @Column(name="cancelled_at") private Instant cancelledAt; @Column(name="cancelled_by") private String cancelledBy;
 @OneToMany(mappedBy="agreement",cascade=CascadeType.ALL,orphanRemoval=true) @OrderBy("sequence ASC,id ASC") private List<AgreementItem> items=new ArrayList<>();
 @Enumerated(EnumType.STRING) @Column(name="billing_start_rule",nullable=false,length=30) private BillingStartRule billingStartRule = BillingStartRule.ISSUE_DATE_INCLUDED;
 @Enumerated(EnumType.STRING) @Column(name="billing_end_rule",nullable=false,length=30) private BillingEndRule billingEndRule = BillingEndRule.RETURN_DATE_EXCLUDED;
 public BillingStartRule getBillingStartRule(){return billingStartRule;} public void setBillingStartRule(BillingStartRule v){billingStartRule=v;}
 public BillingEndRule getBillingEndRule(){return billingEndRule;} public void setBillingEndRule(BillingEndRule v){billingEndRule=v;}
 public Long getId(){return id;} public String getAgreementNumber(){return agreementNumber;} public void setAgreementNumber(String v){agreementNumber=v;}
 public Quotation getQuotation(){return quotation;} public void setQuotation(Quotation v){quotation=v;} public AgreementTemplate getTemplate(){return template;} public void setTemplate(AgreementTemplate v){template=v;}
 public Party getParty(){return party;} public void setParty(Party v){party=v;} public Site getSite(){return site;} public void setSite(Site v){site=v;}
 public LocalDate getAgreementDate(){return agreementDate;} public void setAgreementDate(LocalDate v){agreementDate=v;} public LocalDate getEffectiveDate(){return effectiveDate;} public void setEffectiveDate(LocalDate v){effectiveDate=v;}
 public LocalDate getExpiryDate(){return expiryDate;} public void setExpiryDate(LocalDate v){expiryDate=v;} public RentalType getRentalType(){return rentalType;} public void setRentalType(RentalType v){rentalType=v;}
 public BillingCycle getBillingCycle(){return billingCycle;} public void setBillingCycle(BillingCycle v){billingCycle=v;} public Integer getCustomBillingCycleDays(){return customBillingCycleDays;} public void setCustomBillingCycleDays(Integer v){customBillingCycleDays=v;}
 public MeasurementBasis getMeasurementBasis(){return measurementBasis;} public void setMeasurementBasis(MeasurementBasis v){measurementBasis=v;}
 public BillingCommencementRule getBillingCommencementRule(){return billingCommencementRule;} public void setBillingCommencementRule(BillingCommencementRule v){billingCommencementRule=v;}
 public LocalDate getFixedBillingStartDate(){return fixedBillingStartDate;} public void setFixedBillingStartDate(LocalDate v){fixedBillingStartDate=v;}
 public LocalDate getNextBillingDate(){return nextBillingDate;} public void setNextBillingDate(LocalDate v){nextBillingDate=v;}
 public LocalDate getLastAutoPeriodEnd(){return lastAutoPeriodEnd;} public void setLastAutoPeriodEnd(LocalDate v){lastAutoPeriodEnd=v;}
 public int getGracePeriodDays(){return gracePeriodDays;} public void setGracePeriodDays(int v){gracePeriodDays=v;} public int getMinimumBillingDays(){return minimumBillingDays;} public void setMinimumBillingDays(int v){minimumBillingDays=v;}
 public AgreementStatus getStatus(){return status;} public void setStatus(AgreementStatus v){status=v;}
 public String getPartyLegalNameSnapshot(){return partyLegalNameSnapshot;} public void setPartyLegalNameSnapshot(String v){partyLegalNameSnapshot=v;} public String getPartyTradeNameSnapshot(){return partyTradeNameSnapshot;} public void setPartyTradeNameSnapshot(String v){partyTradeNameSnapshot=v;}
 public String getPartyGstinSnapshot(){return partyGstinSnapshot;} public void setPartyGstinSnapshot(String v){partyGstinSnapshot=v;} public String getPartyPanSnapshot(){return partyPanSnapshot;} public void setPartyPanSnapshot(String v){partyPanSnapshot=v;}
 public String getPartyAddressSnapshot(){return partyAddressSnapshot;} public void setPartyAddressSnapshot(String v){partyAddressSnapshot=v;} public String getPartyStateSnapshot(){return partyStateSnapshot;} public void setPartyStateSnapshot(String v){partyStateSnapshot=v;}
 public String getPartyContactSnapshot(){return partyContactSnapshot;} public void setPartyContactSnapshot(String v){partyContactSnapshot=v;} public String getSiteNameSnapshot(){return siteNameSnapshot;} public void setSiteNameSnapshot(String v){siteNameSnapshot=v;}
 public String getSiteCodeSnapshot(){return siteCodeSnapshot;} public void setSiteCodeSnapshot(String v){siteCodeSnapshot=v;} public String getSiteAddressSnapshot(){return siteAddressSnapshot;} public void setSiteAddressSnapshot(String v){siteAddressSnapshot=v;}
 public String getSiteContactSnapshot(){return siteContactSnapshot;} public void setSiteContactSnapshot(String v){siteContactSnapshot=v;} public String getQuotationNumberSnapshot(){return quotationNumberSnapshot;} public void setQuotationNumberSnapshot(String v){quotationNumberSnapshot=v;}
 public LocalDate getQuotationDateSnapshot(){return quotationDateSnapshot;} public void setQuotationDateSnapshot(LocalDate v){quotationDateSnapshot=v;} public Instant getQuotationApprovedAtSnapshot(){return quotationApprovedAtSnapshot;} public void setQuotationApprovedAtSnapshot(Instant v){quotationApprovedAtSnapshot=v;}
 public BigDecimal getSecurityDeposit(){return securityDeposit;} public void setSecurityDeposit(BigDecimal v){securityDeposit=v;} public BigDecimal getSubtotal(){return subtotal;} public void setSubtotal(BigDecimal v){subtotal=v;}
 public BigDecimal getDiscountAmount(){return discountAmount;} public void setDiscountAmount(BigDecimal v){discountAmount=v;} public BigDecimal getTaxableAmount(){return taxableAmount;} public void setTaxableAmount(BigDecimal v){taxableAmount=v;}
 public BigDecimal getCgstAmount(){return cgstAmount;} public void setCgstAmount(BigDecimal v){cgstAmount=v;} public BigDecimal getSgstAmount(){return sgstAmount;} public void setSgstAmount(BigDecimal v){sgstAmount=v;}
 public BigDecimal getIgstAmount(){return igstAmount;} public void setIgstAmount(BigDecimal v){igstAmount=v;} public BigDecimal getTotalTax(){return totalTax;} public void setTotalTax(BigDecimal v){totalTax=v;}
 public BigDecimal getTransportCharge(){return transportCharge;} public void setTransportCharge(BigDecimal v){transportCharge=v;} public BigDecimal getLoadingCharge(){return loadingCharge;} public void setLoadingCharge(BigDecimal v){loadingCharge=v;}
 public String getHeaderText(){return headerText;} public void setHeaderText(String v){headerText=v;}
 public BigDecimal getUnloadingCharge(){return unloadingCharge;} public void setUnloadingCharge(BigDecimal v){unloadingCharge=v;} public BigDecimal getOtherCharge(){return otherCharge;} public void setOtherCharge(BigDecimal v){otherCharge=v;}
 public BigDecimal getRoundOff(){return roundOff;} public void setRoundOff(BigDecimal v){roundOff=v;} public BigDecimal getGrandTotal(){return grandTotal;} public void setGrandTotal(BigDecimal v){grandTotal=v;}
 public String getTerms(){return terms;} public void setTerms(String v){terms=v;} public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
 public FileAttachment getGeneratedDocument(){return generatedDocument;} public void setGeneratedDocument(FileAttachment v){generatedDocument=v;} public String getGeneratedFilename(){return generatedFilename;} public void setGeneratedFilename(String v){generatedFilename=v;}
 public String getGeneratedStoragePath(){return generatedStoragePath;} public void setGeneratedStoragePath(String v){generatedStoragePath=v;} public Instant getGeneratedAt(){return generatedAt;} public void setGeneratedAt(Instant v){generatedAt=v;}
 public FileAttachment getSignedDocument(){return signedDocument;} public void setSignedDocument(FileAttachment v){signedDocument=v;}
 public String getSignedFilename(){return signedFilename;} public void setSignedFilename(String v){signedFilename=v;}
 public Instant getSignedUploadedAt(){return signedUploadedAt;} public void setSignedUploadedAt(Instant v){signedUploadedAt=v;}
 public Instant getReadyForReviewAt(){return readyForReviewAt;} public void setReadyForReviewAt(Instant v){readyForReviewAt=v;} public String getReadyForReviewBy(){return readyForReviewBy;} public void setReadyForReviewBy(String v){readyForReviewBy=v;}
 public Instant getActivatedAt(){return activatedAt;} public void setActivatedAt(Instant v){activatedAt=v;} public String getActivatedBy(){return activatedBy;} public void setActivatedBy(String v){activatedBy=v;}
 public Instant getExpiredAt(){return expiredAt;} public void setExpiredAt(Instant v){expiredAt=v;} public String getExpiredBy(){return expiredBy;} public void setExpiredBy(String v){expiredBy=v;}
 public String getTerminationReason(){return terminationReason;} public void setTerminationReason(String v){terminationReason=v;} public Instant getTerminatedAt(){return terminatedAt;} public void setTerminatedAt(Instant v){terminatedAt=v;} public String getTerminatedBy(){return terminatedBy;} public void setTerminatedBy(String v){terminatedBy=v;}
 public Instant getClosedAt(){return closedAt;} public void setClosedAt(Instant v){closedAt=v;} public String getClosedBy(){return closedBy;} public void setClosedBy(String v){closedBy=v;}
 public String getCancellationReason(){return cancellationReason;} public void setCancellationReason(String v){cancellationReason=v;} public Instant getCancelledAt(){return cancelledAt;} public void setCancelledAt(Instant v){cancelledAt=v;} public String getCancelledBy(){return cancelledBy;} public void setCancelledBy(String v){cancelledBy=v;}
 public List<AgreementItem> getItems(){return items;} public void replaceItems(List<AgreementItem> v){items.clear();v.forEach(this::addItem);} public void addItem(AgreementItem i){i.setAgreement(this);items.add(i);}
}
