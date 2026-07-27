package com.stocksync.billing.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "billing_run_charges")
public class BillingRunCharge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_run_id")
    private BillingRun billingRun;

    @Column(name = "source_type", nullable = false, length = 40)
    private String sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "source_document_number", nullable = false, length = 50)
    private String sourceDocumentNumber;

    @Column(name = "charge_type", nullable = false, length = 40)
    private String chargeType;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean taxable = true;

    @Column(nullable = false)
    private boolean selected = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BillingRun getBillingRun() { return billingRun; }
    public void setBillingRun(BillingRun billingRun) { this.billingRun = billingRun; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getSourceDocumentNumber() { return sourceDocumentNumber; }
    public void setSourceDocumentNumber(String sourceDocumentNumber) { this.sourceDocumentNumber = sourceDocumentNumber; }
    public String getChargeType() { return chargeType; }
    public void setChargeType(String chargeType) { this.chargeType = chargeType; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public boolean isTaxable() { return taxable; }
    public void setTaxable(boolean taxable) { this.taxable = taxable; }
    public boolean isSelected() { return selected; }
    public void setSelected(boolean selected) { this.selected = selected; }
}
