package com.stocksync.billing.entity;

import com.stocksync.agreement.entity.AgreementItem;
import com.stocksync.inventory.entity.Item;
import com.stocksync.quotation.entity.RentalType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "billing_run_segments")
public class BillingRunSegment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_run_id")
    private BillingRun billingRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_item_id")
    private AgreementItem agreementItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "item_code_snapshot", nullable = false, length = 50)
    private String itemCodeSnapshot;

    @Column(name = "item_name_snapshot", nullable = false)
    private String itemNameSnapshot;

    @Column(name = "size_snapshot", length = 50)
    private String sizeSnapshot;

    @Column(name = "unit_snapshot", nullable = false, length = 20)
    private String unitSnapshot;

    @Column(name = "weight_snapshot", precision = 19, scale = 4)
    private BigDecimal weightSnapshot;

    @Column(name = "source_issue_reference", nullable = false, length = 100)
    private String sourceIssueReference;

    @Column(name = "source_end_reference", length = 100)
    private String sourceEndReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "rental_type", nullable = false, length = 40)
    private RentalType rentalType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(precision = 19, scale = 4)
    private BigDecimal area;

    @Column(precision = 19, scale = 4)
    private BigDecimal weight;

    @Column(name = "segment_start", nullable = false)
    private LocalDate segmentStart;

    @Column(name = "segment_end", nullable = false)
    private LocalDate segmentEnd;

    @Column(name = "billable_days", nullable = false)
    private int billableDays;

    @Column(name = "base_rate", nullable = false, precision = 19, scale = 4)
    private BigDecimal baseRate = BigDecimal.ZERO;

    @Column(name = "applied_slab_snapshot", length = 500)
    private String appliedSlabSnapshot;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "calculation_explanation", nullable = false, length = 1000)
    private String calculationExplanation;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BillingRun getBillingRun() { return billingRun; }
    public void setBillingRun(BillingRun billingRun) { this.billingRun = billingRun; }
    public AgreementItem getAgreementItem() { return agreementItem; }
    public void setAgreementItem(AgreementItem agreementItem) { this.agreementItem = agreementItem; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public String getItemCodeSnapshot() { return itemCodeSnapshot; }
    public void setItemCodeSnapshot(String itemCodeSnapshot) { this.itemCodeSnapshot = itemCodeSnapshot; }
    public String getItemNameSnapshot() { return itemNameSnapshot; }
    public void setItemNameSnapshot(String itemNameSnapshot) { this.itemNameSnapshot = itemNameSnapshot; }
    public String getSizeSnapshot() { return sizeSnapshot; }
    public void setSizeSnapshot(String sizeSnapshot) { this.sizeSnapshot = sizeSnapshot; }
    public String getUnitSnapshot() { return unitSnapshot; }
    public void setUnitSnapshot(String unitSnapshot) { this.unitSnapshot = unitSnapshot; }
    public BigDecimal getWeightSnapshot() { return weightSnapshot; }
    public void setWeightSnapshot(BigDecimal weightSnapshot) { this.weightSnapshot = weightSnapshot; }
    public String getSourceIssueReference() { return sourceIssueReference; }
    public void setSourceIssueReference(String sourceIssueReference) { this.sourceIssueReference = sourceIssueReference; }
    public String getSourceEndReference() { return sourceEndReference; }
    public void setSourceEndReference(String sourceEndReference) { this.sourceEndReference = sourceEndReference; }
    public RentalType getRentalType() { return rentalType; }
    public void setRentalType(RentalType rentalType) { this.rentalType = rentalType; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public LocalDate getSegmentStart() { return segmentStart; }
    public void setSegmentStart(LocalDate segmentStart) { this.segmentStart = segmentStart; }
    public LocalDate getSegmentEnd() { return segmentEnd; }
    public void setSegmentEnd(LocalDate segmentEnd) { this.segmentEnd = segmentEnd; }
    public int getBillableDays() { return billableDays; }
    public void setBillableDays(int billableDays) { this.billableDays = billableDays; }
    public BigDecimal getBaseRate() { return baseRate; }
    public void setBaseRate(BigDecimal baseRate) { this.baseRate = baseRate; }
    public String getAppliedSlabSnapshot() { return appliedSlabSnapshot; }
    public void setAppliedSlabSnapshot(String appliedSlabSnapshot) { this.appliedSlabSnapshot = appliedSlabSnapshot; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCalculationExplanation() { return calculationExplanation; }
    public void setCalculationExplanation(String calculationExplanation) { this.calculationExplanation = calculationExplanation; }
    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }
}
