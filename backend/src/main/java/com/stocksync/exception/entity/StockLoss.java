package com.stocksync.exception.entity;

import com.stocksync.agreement.entity.Agreement;
import com.stocksync.challan.entity.ReceivingChallan;
import com.stocksync.challan.entity.ReceivingChallanItem;
import com.stocksync.common.persistence.AuditedEntity;
import com.stocksync.file.entity.FileAttachment;
import com.stocksync.inventory.entity.Item;
import com.stocksync.party.entity.Party;
import com.stocksync.site.entity.Site;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "loss_records")
public class StockLoss extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loss_number", nullable = false, unique = true, length = 50)
    private String lossNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private ExceptionSourceType sourceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_receiving_challan_id")
    private ReceivingChallan sourceReceivingChallan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_receiving_challan_item_id")
    private ReceivingChallanItem sourceReceivingChallanItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id")
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "loss_date", nullable = false)
    private LocalDate lossDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal weight = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_method", nullable = false, length = 30)
    private ChargeMethod chargeMethod;

    @Column(name = "recovery_rate", nullable = false, precision = 19, scale = 4)
    private BigDecimal recoveryRate = BigDecimal.ZERO;

    @Column(name = "calculated_recovery_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal calculatedRecoveryAmount = BigDecimal.ZERO;

    @Column(length = 500)
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id")
    private FileAttachment attachment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LossStatus status = LossStatus.DRAFT;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by", length = 50)
    private String approvedBy;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Column(name = "reversed_by", length = 50)
    private String reversedBy;

    @Column(name = "reversal_reason")
    private String reversalReason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getLossNumber() { return lossNumber; }
    public void setLossNumber(String lossNumber) { this.lossNumber = lossNumber; }
    public ExceptionSourceType getSourceType() { return sourceType; }
    public void setSourceType(ExceptionSourceType sourceType) { this.sourceType = sourceType; }
    public ReceivingChallan getSourceReceivingChallan() { return sourceReceivingChallan; }
    public void setSourceReceivingChallan(ReceivingChallan sourceReceivingChallan) { this.sourceReceivingChallan = sourceReceivingChallan; }
    public ReceivingChallanItem getSourceReceivingChallanItem() { return sourceReceivingChallanItem; }
    public void setSourceReceivingChallanItem(ReceivingChallanItem sourceReceivingChallanItem) { this.sourceReceivingChallanItem = sourceReceivingChallanItem; }
    public Agreement getAgreement() { return agreement; }
    public void setAgreement(Agreement agreement) { this.agreement = agreement; }
    public Party getParty() { return party; }
    public void setParty(Party party) { this.party = party; }
    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public LocalDate getLossDate() { return lossDate; }
    public void setLossDate(LocalDate lossDate) { this.lossDate = lossDate; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public ChargeMethod getChargeMethod() { return chargeMethod; }
    public void setChargeMethod(ChargeMethod chargeMethod) { this.chargeMethod = chargeMethod; }
    public BigDecimal getRecoveryRate() { return recoveryRate; }
    public void setRecoveryRate(BigDecimal recoveryRate) { this.recoveryRate = recoveryRate; }
    public BigDecimal getCalculatedRecoveryAmount() { return calculatedRecoveryAmount; }
    public void setCalculatedRecoveryAmount(BigDecimal calculatedRecoveryAmount) { this.calculatedRecoveryAmount = calculatedRecoveryAmount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public FileAttachment getAttachment() { return attachment; }
    public void setAttachment(FileAttachment attachment) { this.attachment = attachment; }
    public LossStatus getStatus() { return status; }
    public void setStatus(LossStatus status) { this.status = status; }
    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public Instant getReversedAt() { return reversedAt; }
    public void setReversedAt(Instant reversedAt) { this.reversedAt = reversedAt; }
    public String getReversedBy() { return reversedBy; }
    public void setReversedBy(String reversedBy) { this.reversedBy = reversedBy; }
    public String getReversalReason() { return reversalReason; }
    public void setReversalReason(String reversalReason) { this.reversalReason = reversalReason; }
}
