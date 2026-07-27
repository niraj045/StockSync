package com.stocksync.billing.entity;

import com.stocksync.agreement.entity.Agreement;
import com.stocksync.common.persistence.AuditedEntity;
import com.stocksync.party.entity.Party;
import com.stocksync.quotation.entity.DiscountType;
import com.stocksync.site.entity.Site;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "billing_runs")
public class BillingRun extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "billing_run_number", nullable = false, unique = true, length = 50)
    private String billingRunNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BillingRunStatus status = BillingRunStatus.DRAFT;

    @Column(name = "rental_subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal rentalSubtotal = BigDecimal.ZERO;

    @Column(name = "loss_charge_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal lossChargeTotal = BigDecimal.ZERO;

    @Column(name = "damage_charge_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal damageChargeTotal = BigDecimal.ZERO;

    @Column(name = "operational_charge_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal operationalChargeTotal = BigDecimal.ZERO;

    @Column(name = "manual_adjustment_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal manualAdjustmentTotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType = DiscountType.NONE;

    @Column(name = "discount_value", nullable = false, precision = 19, scale = 4)
    private BigDecimal discountValue = BigDecimal.ZERO;

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

    @Column(name = "calculated_at")
    private Instant calculatedAt;

    @Column(name = "calculated_by", length = 50)
    private String calculatedBy;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Column(name = "finalized_by", length = 50)
    private String finalizedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 50)
    private String cancelledBy;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @OneToMany(mappedBy = "billingRun", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceNumber ASC")
    private List<BillingRunSegment> segments = new ArrayList<>();

    @OneToMany(mappedBy = "billingRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BillingRunCharge> charges = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBillingRunNumber() { return billingRunNumber; }
    public void setBillingRunNumber(String billingRunNumber) { this.billingRunNumber = billingRunNumber; }
    public Agreement getAgreement() { return agreement; }
    public void setAgreement(Agreement agreement) { this.agreement = agreement; }
    public Party getParty() { return party; }
    public void setParty(Party party) { this.party = party; }
    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
    public BillingRunStatus getStatus() { return status; }
    public void setStatus(BillingRunStatus status) { this.status = status; }
    public BigDecimal getRentalSubtotal() { return rentalSubtotal; }
    public void setRentalSubtotal(BigDecimal rentalSubtotal) { this.rentalSubtotal = rentalSubtotal; }
    public BigDecimal getLossChargeTotal() { return lossChargeTotal; }
    public void setLossChargeTotal(BigDecimal lossChargeTotal) { this.lossChargeTotal = lossChargeTotal; }
    public BigDecimal getDamageChargeTotal() { return damageChargeTotal; }
    public void setDamageChargeTotal(BigDecimal damageChargeTotal) { this.damageChargeTotal = damageChargeTotal; }
    public BigDecimal getOperationalChargeTotal() { return operationalChargeTotal; }
    public void setOperationalChargeTotal(BigDecimal operationalChargeTotal) { this.operationalChargeTotal = operationalChargeTotal; }
    public BigDecimal getManualAdjustmentTotal() { return manualAdjustmentTotal; }
    public void setManualAdjustmentTotal(BigDecimal manualAdjustmentTotal) { this.manualAdjustmentTotal = manualAdjustmentTotal; }
    public DiscountType getDiscountType() { return discountType; }
    public void setDiscountType(DiscountType discountType) { this.discountType = discountType; }
    public BigDecimal getDiscountValue() { return discountValue; }
    public void setDiscountValue(BigDecimal discountValue) { this.discountValue = discountValue; }
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
    public Instant getCalculatedAt() { return calculatedAt; }
    public void setCalculatedAt(Instant calculatedAt) { this.calculatedAt = calculatedAt; }
    public String getCalculatedBy() { return calculatedBy; }
    public void setCalculatedBy(String calculatedBy) { this.calculatedBy = calculatedBy; }
    public Instant getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(Instant finalizedAt) { this.finalizedAt = finalizedAt; }
    public String getFinalizedBy() { return finalizedBy; }
    public void setFinalizedBy(String finalizedBy) { this.finalizedBy = finalizedBy; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public List<BillingRunSegment> getSegments() { return segments; }
    public void setSegments(List<BillingRunSegment> segments) { this.segments = segments; }
    public List<BillingRunCharge> getCharges() { return charges; }
    public void setCharges(List<BillingRunCharge> charges) { this.charges = charges; }


    public String getPartyLegalNameSnapshot() {
        return party != null ? party.getLegalName() : null;
    }

    public String getSiteNameSnapshot() {
        return site != null ? site.getSiteName() : null;
    }

    public void addSegment(BillingRunSegment seg) {
        seg.setBillingRun(this);
        this.segments.add(seg);
    }

    public void addCharge(BillingRunCharge charge) {
        charge.setBillingRun(this);
        this.charges.add(charge);
    }
}
