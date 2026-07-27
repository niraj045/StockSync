package com.stocksync.exception.entity;

import com.stocksync.agreement.entity.Agreement;
import com.stocksync.challan.entity.ReceivingChallan;
import com.stocksync.challan.entity.ReceivingChallanItem;
import com.stocksync.common.persistence.AuditedEntity;
import com.stocksync.inventory.entity.Item;
import com.stocksync.party.entity.Party;
import com.stocksync.site.entity.Site;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(name = "item_exchange_records")
public class ItemExchange extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exchange_number", nullable = false, unique = true, length = 50)
    private String exchangeNumber;

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
    @JoinColumn(name = "expected_item_id")
    private Item expectedItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actual_item_id")
    private Item actualItem;

    @Column(name = "exchange_date", nullable = false)
    private LocalDate exchangeDate;

    @Column(name = "expected_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal expectedQuantity = BigDecimal.ZERO;

    @Column(name = "actual_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal actualQuantity = BigDecimal.ZERO;

    @Column(name = "expected_weight", nullable = false, precision = 19, scale = 4)
    private BigDecimal expectedWeight = BigDecimal.ZERO;

    @Column(name = "actual_weight", nullable = false, precision = 19, scale = 4)
    private BigDecimal actualWeight = BigDecimal.ZERO;

    @Column(name = "destination_stock_status", nullable = false, length = 30)
    private String destinationStockStatus = "AVAILABLE"; // AVAILABLE, DAMAGED

    @Column(length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExchangeStatus status = ExchangeStatus.DRAFT;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "posted_by", length = 50)
    private String postedBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by", length = 50)
    private String cancelledBy;

    @Column(name = "cancellation_reason")
    private String cancellationReason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getExchangeNumber() { return exchangeNumber; }
    public void setExchangeNumber(String exchangeNumber) { this.exchangeNumber = exchangeNumber; }
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
    public Item getExpectedItem() { return expectedItem; }
    public void setExpectedItem(Item expectedItem) { this.expectedItem = expectedItem; }
    public Item getActualItem() { return actualItem; }
    public void setActualItem(Item actualItem) { this.actualItem = actualItem; }
    public LocalDate getExchangeDate() { return exchangeDate; }
    public void setExchangeDate(LocalDate exchangeDate) { this.exchangeDate = exchangeDate; }
    public BigDecimal getExpectedQuantity() { return expectedQuantity; }
    public void setExpectedQuantity(BigDecimal expectedQuantity) { this.expectedQuantity = expectedQuantity; }
    public BigDecimal getActualQuantity() { return actualQuantity; }
    public void setActualQuantity(BigDecimal actualQuantity) { this.actualQuantity = actualQuantity; }
    public BigDecimal getExpectedWeight() { return expectedWeight; }
    public void setExpectedWeight(BigDecimal expectedWeight) { this.expectedWeight = expectedWeight; }
    public BigDecimal getActualWeight() { return actualWeight; }
    public void setActualWeight(BigDecimal actualWeight) { this.actualWeight = actualWeight; }
    public String getDestinationStockStatus() { return destinationStockStatus; }
    public void setDestinationStockStatus(String destinationStockStatus) { this.destinationStockStatus = destinationStockStatus; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public ExchangeStatus getStatus() { return status; }
    public void setStatus(ExchangeStatus status) { this.status = status; }
    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }
    public String getPostedBy() { return postedBy; }
    public void setPostedBy(String postedBy) { this.postedBy = postedBy; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String cancelledBy) { this.cancelledBy = cancelledBy; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
}
