package com.stocksync.billing.entity;

import com.stocksync.agreement.entity.Agreement;
import jakarta.persistence.*;

@Entity
@Table(name = "billing_source_allocations", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"source_type", "source_id"})
})
public class BillingSourceAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_run_id")
    private BillingRun billingRun;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Agreement getAgreement() { return agreement; }
    public void setAgreement(Agreement agreement) { this.agreement = agreement; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public BillingRun getBillingRun() { return billingRun; }
    public void setBillingRun(BillingRun billingRun) { this.billingRun = billingRun; }
}
