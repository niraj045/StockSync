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
@Table(name = "damage_records")
public class StockDamage extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "damage_number", nullable = false, unique = true, length = 50)
    private String damageNumber;

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

    @Column(name = "damage_date", nullable = false)
    private LocalDate damageDate;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal weight = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean repairable = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "damage_type", nullable = false, length = 30)
    private DamageType damageType;

    @Column(name = "condition_notes", length = 500)
    private String conditionNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_method", nullable = false, length = 30)
    private ChargeMethod chargeMethod;

    @Column(name = "damage_rate", nullable = false, precision = 19, scale = 4)
    private BigDecimal damageRate = BigDecimal.ZERO;

    @Column(name = "calculated_damage_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal calculatedDamageAmount = BigDecimal.ZERO;

    @Column(name = "estimated_repair_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal estimatedRepairCost = BigDecimal.ZERO;

    @Column(name = "actual_repair_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal actualRepairCost = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attachment_id")
    private FileAttachment attachment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DamageStatus status = DamageStatus.DRAFT;

    @Column(name = "recorded_at")
    private Instant recordedAt;

    @Column(name = "recorded_by", length = 50)
    private String recordedBy;

    @Column(name = "repair_started_at")
    private Instant repairStartedAt;

    @Column(name = "repair_started_by", length = 50)
    private String repairStartedBy;

    @Column(name = "repaired_at")
    private Instant repairedAt;

    @Column(name = "repaired_by", length = 50)
    private String repairedBy;

    @Column(name = "scrapped_at")
    private Instant scrappedAt;

    @Column(name = "scrapped_by", length = 50)
    private String scrappedBy;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Column(name = "reversed_by", length = 50)
    private String reversedBy;

    @Column(name = "reversal_reason")
    private String reversalReason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDamageNumber() { return damageNumber; }
    public void setDamageNumber(String damageNumber) { this.damageNumber = damageNumber; }
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
    public LocalDate getDamageDate() { return damageDate; }
    public void setDamageDate(LocalDate damageDate) { this.damageDate = damageDate; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public boolean isRepairable() { return repairable; }
    public void setRepairable(boolean repairable) { this.repairable = repairable; }
    public DamageType getDamageType() { return damageType; }
    public void setDamageType(DamageType damageType) { this.damageType = damageType; }
    public String getConditionNotes() { return conditionNotes; }
    public void setConditionNotes(String conditionNotes) { this.conditionNotes = conditionNotes; }
    public ChargeMethod getChargeMethod() { return chargeMethod; }
    public void setChargeMethod(ChargeMethod chargeMethod) { this.chargeMethod = chargeMethod; }
    public BigDecimal getDamageRate() { return damageRate; }
    public void setDamageRate(BigDecimal damageRate) { this.damageRate = damageRate; }
    public BigDecimal getCalculatedDamageAmount() { return calculatedDamageAmount; }
    public void setCalculatedDamageAmount(BigDecimal calculatedDamageAmount) { this.calculatedDamageAmount = calculatedDamageAmount; }
    public BigDecimal getEstimatedRepairCost() { return estimatedRepairCost; }
    public void setEstimatedRepairCost(BigDecimal estimatedRepairCost) { this.estimatedRepairCost = estimatedRepairCost; }
    public BigDecimal getActualRepairCost() { return actualRepairCost; }
    public void setActualRepairCost(BigDecimal actualRepairCost) { this.actualRepairCost = actualRepairCost; }
    public FileAttachment getAttachment() { return attachment; }
    public void setAttachment(FileAttachment attachment) { this.attachment = attachment; }
    public DamageStatus getStatus() { return status; }
    public void setStatus(DamageStatus status) { this.status = status; }
    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
    public String getRecordedBy() { return recordedBy; }
    public void setRecordedBy(String recordedBy) { this.recordedBy = recordedBy; }
    public Instant getRepairStartedAt() { return repairStartedAt; }
    public void setRepairStartedAt(Instant repairStartedAt) { this.repairStartedAt = repairStartedAt; }
    public String getRepairStartedBy() { return repairStartedBy; }
    public void setRepairStartedBy(String repairStartedBy) { this.repairStartedBy = repairStartedBy; }
    public Instant getRepairedAt() { return repairedAt; }
    public void setRepairedAt(Instant repairedAt) { this.repairedAt = repairedAt; }
    public String getRepairedBy() { return repairedBy; }
    public void setRepairedBy(String repairedBy) { this.repairedBy = repairedBy; }
    public Instant getScrappedAt() { return scrappedAt; }
    public void setScrappedAt(Instant scrappedAt) { this.scrappedAt = scrappedAt; }
    public String getScrappedBy() { return scrappedBy; }
    public void setScrappedBy(String scrappedBy) { this.scrappedBy = scrappedBy; }
    public Instant getReversedAt() { return reversedAt; }
    public void setReversedAt(Instant reversedAt) { this.reversedAt = reversedAt; }
    public String getReversedBy() { return reversedBy; }
    public void setReversedBy(String reversedBy) { this.reversedBy = reversedBy; }
    public String getReversalReason() { return reversalReason; }
    public void setReversalReason(String reversalReason) { this.reversalReason = reversalReason; }
}
