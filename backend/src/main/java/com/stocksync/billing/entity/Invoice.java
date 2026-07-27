package com.stocksync.billing.entity;

import com.stocksync.agreement.entity.Agreement;
import com.stocksync.common.persistence.AuditedEntity;
import com.stocksync.file.entity.FileAttachment;
import com.stocksync.party.entity.Party;
import com.stocksync.site.entity.Site;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "invoices")
public class Invoice extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_run_id", unique = true)
    private BillingRun billingRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(name = "company_name_snapshot", nullable = false, length = 150)
    private String companyNameSnapshot;

    @Column(name = "company_address_snapshot", nullable = false, length = 500)
    private String companyAddressSnapshot;

    @Column(name = "company_gstin_snapshot", nullable = false, length = 15)
    private String companyGstinSnapshot;

    @Column(name = "party_legal_name_snapshot", nullable = false)
    private String partyLegalNameSnapshot;

    @Column(name = "party_gstin_snapshot", length = 15)
    private String partyGstinSnapshot;

    @Column(name = "party_pan_snapshot", length = 10)
    private String partyPanSnapshot;

    @Column(name = "party_address_snapshot", nullable = false, length = 500)
    private String partyAddressSnapshot;

    @Column(name = "party_state_snapshot", nullable = false, length = 100)
    private String partyStateSnapshot;

    @Column(name = "site_name_snapshot", nullable = false)
    private String siteNameSnapshot;

    @Column(name = "site_code_snapshot", nullable = false, length = 50)
    private String siteCodeSnapshot;

    @Column(name = "site_address_snapshot", nullable = false, length = 500)
    private String siteAddressSnapshot;

    @Column(name = "site_contact_snapshot")
    private String siteContactSnapshot;

    @Column(name = "agreement_number_snapshot", nullable = false, length = 50)
    private String agreementNumberSnapshot;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "discount_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "taxable_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal taxableAmount = BigDecimal.ZERO;

    @Column(name = "cgst_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal cgstRate = BigDecimal.ZERO;

    @Column(name = "cgst_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal cgstAmount = BigDecimal.ZERO;

    @Column(name = "sgst_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal sgstRate = BigDecimal.ZERO;

    @Column(name = "sgst_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal sgstAmount = BigDecimal.ZERO;

    @Column(name = "igst_rate", nullable = false, precision = 7, scale = 4)
    private BigDecimal igstRate = BigDecimal.ZERO;

    @Column(name = "igst_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal igstAmount = BigDecimal.ZERO;

    @Column(name = "total_tax", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Column(name = "round_off", nullable = false, precision = 19, scale = 2)
    private BigDecimal roundOff = BigDecimal.ZERO;

    @Column(name = "grand_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal grandTotal = BigDecimal.ZERO;

    @Column(name = "cash_allocated_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal cashAllocatedTotal = BigDecimal.ZERO;

    @Column(name = "tds_allocated_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal tdsAllocatedTotal = BigDecimal.ZERO;

    @Column(name = "deposit_adjusted_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal depositAdjustedTotal = BigDecimal.ZERO;

    @Column(name = "outstanding_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingAmount = BigDecimal.ZERO;

    @Column(length = 4000)
    private String terms;

    @Column(length = 1000)
    private String notes;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_pdf_attachment_id")
    private FileAttachment generatedPdf;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "issued_by", length = 50)
    private String issuedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 50)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC")
    private List<InvoiceItem> items = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public BillingRun getBillingRun() { return billingRun; }
    public void setBillingRun(BillingRun billingRun) { this.billingRun = billingRun; }
    public Agreement getAgreement() { return agreement; }
    public void setAgreement(Agreement agreement) { this.agreement = agreement; }
    public Party getParty() { return party; }
    public void setParty(Party party) { this.party = party; }
    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public void setInvoiceDate(LocalDate invoiceDate) { this.invoiceDate = invoiceDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public InvoiceStatus getStatus() { return status; }
    public void setStatus(InvoiceStatus status) { this.status = status; }
    public String getCompanyNameSnapshot() { return companyNameSnapshot; }
    public void setCompanyNameSnapshot(String companyNameSnapshot) { this.companyNameSnapshot = companyNameSnapshot; }
    public String getCompanyAddressSnapshot() { return companyAddressSnapshot; }
    public void setCompanyAddressSnapshot(String companyAddressSnapshot) { this.companyAddressSnapshot = companyAddressSnapshot; }
    public String getCompanyGstinSnapshot() { return companyGstinSnapshot; }
    public void setCompanyGstinSnapshot(String companyGstinSnapshot) { this.companyGstinSnapshot = companyGstinSnapshot; }
    public String getPartyLegalNameSnapshot() { return partyLegalNameSnapshot; }
    public void setPartyLegalNameSnapshot(String partyLegalNameSnapshot) { this.partyLegalNameSnapshot = partyLegalNameSnapshot; }
    public String getPartyGstinSnapshot() { return partyGstinSnapshot; }
    public void setPartyGstinSnapshot(String partyGstinSnapshot) { this.partyGstinSnapshot = partyGstinSnapshot; }
    public String getPartyPanSnapshot() { return partyPanSnapshot; }
    public void setPartyPanSnapshot(String partyPanSnapshot) { this.partyPanSnapshot = partyPanSnapshot; }
    public String getPartyAddressSnapshot() { return partyAddressSnapshot; }
    public void setPartyAddressSnapshot(String partyAddressSnapshot) { this.partyAddressSnapshot = partyAddressSnapshot; }
    public String getPartyStateSnapshot() { return partyStateSnapshot; }
    public void setPartyStateSnapshot(String partyStateSnapshot) { this.partyStateSnapshot = partyStateSnapshot; }
    public String getSiteNameSnapshot() { return siteNameSnapshot; }
    public void setSiteNameSnapshot(String siteNameSnapshot) { this.siteNameSnapshot = siteNameSnapshot; }
    public String getSiteCodeSnapshot() { return siteCodeSnapshot; }
    public void setSiteCodeSnapshot(String siteCodeSnapshot) { this.siteCodeSnapshot = siteCodeSnapshot; }
    public String getSiteAddressSnapshot() { return siteAddressSnapshot; }
    public void setSiteAddressSnapshot(String siteAddressSnapshot) { this.siteAddressSnapshot = siteAddressSnapshot; }
    public String getSiteContactSnapshot() { return siteContactSnapshot; }
    public void setSiteContactSnapshot(String siteContactSnapshot) { this.siteContactSnapshot = siteContactSnapshot; }
    public String getAgreementNumberSnapshot() { return agreementNumberSnapshot; }
    public void setAgreementNumberSnapshot(String agreementNumberSnapshot) { this.agreementNumberSnapshot = agreementNumberSnapshot; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getTaxableAmount() { return taxableAmount; }
    public void setTaxableAmount(BigDecimal taxableAmount) { this.taxableAmount = taxableAmount; }
    public BigDecimal getCgstRate() { return cgstRate; }
    public void setCgstRate(BigDecimal cgstRate) { this.cgstRate = cgstRate; }
    public BigDecimal getCgstAmount() { return cgstAmount; }
    public void setCgstAmount(BigDecimal cgstAmount) { this.cgstAmount = cgstAmount; }
    public BigDecimal getSgstRate() { return sgstRate; }
    public void setSgstRate(BigDecimal sgstRate) { this.sgstRate = sgstRate; }
    public BigDecimal getSgstAmount() { return sgstAmount; }
    public void setSgstAmount(BigDecimal sgstAmount) { this.sgstAmount = sgstAmount; }
    public BigDecimal getIgstRate() { return igstRate; }
    public void setIgstRate(BigDecimal igstRate) { this.igstRate = igstRate; }
    public BigDecimal getIgstAmount() { return igstAmount; }
    public void setIgstAmount(BigDecimal igstAmount) { this.igstAmount = igstAmount; }
    public BigDecimal getTotalTax() { return totalTax; }
    public void setTotalTax(BigDecimal totalTax) { this.totalTax = totalTax; }
    public BigDecimal getRoundOff() { return roundOff; }
    public void setRoundOff(BigDecimal roundOff) { this.roundOff = roundOff; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }
    public BigDecimal getCashAllocatedTotal() { return cashAllocatedTotal; }
    public void setCashAllocatedTotal(BigDecimal cashAllocatedTotal) { this.cashAllocatedTotal = cashAllocatedTotal; }
    public BigDecimal getTdsAllocatedTotal() { return tdsAllocatedTotal; }
    public void setTdsAllocatedTotal(BigDecimal tdsAllocatedTotal) { this.tdsAllocatedTotal = tdsAllocatedTotal; }
    public BigDecimal getDepositAdjustedTotal() { return depositAdjustedTotal; }
    public void setDepositAdjustedTotal(BigDecimal depositAdjustedTotal) { this.depositAdjustedTotal = depositAdjustedTotal; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(BigDecimal outstandingAmount) { this.outstandingAmount = outstandingAmount; }
    public String getTerms() { return terms; }
    public void setTerms(String terms) { this.terms = terms; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public FileAttachment getGeneratedPdf() { return generatedPdf; }
    public void setGeneratedPdf(FileAttachment generatedPdf) { this.generatedPdf = generatedPdf; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
    public String getIssuedBy() { return issuedBy; }
    public void setIssuedBy(String issuedBy) { this.issuedBy = issuedBy; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public List<InvoiceItem> getItems() { return items; }
    public void setItems(List<InvoiceItem> items) { this.items = items; }


    public void addItem(InvoiceItem item) {
        item.setInvoice(this);
        this.items.add(item);
    }
}
