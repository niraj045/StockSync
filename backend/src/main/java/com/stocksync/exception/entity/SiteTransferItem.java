package com.stocksync.exception.entity;

import com.stocksync.agreement.entity.AgreementItem;
import com.stocksync.inventory.entity.Item;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "site_transfer_items")
public class SiteTransferItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transfer_id")
    private SiteTransfer transfer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_agreement_item_id")
    private AgreementItem sourceAgreementItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_agreement_item_id")
    private AgreementItem destinationAgreementItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "item_code_snapshot", nullable = false, length = 50)
    private String itemCodeSnapshot;

    @Column(name = "item_name_snapshot", nullable = false)
    private String itemNameSnapshot;

    @Column(name = "description_snapshot", length = 500)
    private String descriptionSnapshot;

    @Column(name = "size_snapshot", length = 50)
    private String sizeSnapshot;

    @Column(name = "unit_snapshot", nullable = false, length = 20)
    private String unitSnapshot;

    @Column(name = "weight_per_piece_snapshot", nullable = false, precision = 19, scale = 4)
    private BigDecimal weightPerPieceSnapshot = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "total_weight", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalWeight = BigDecimal.ZERO;

    @Column(name = "source_pending_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal sourcePendingBefore = BigDecimal.ZERO;

    @Column(name = "source_pending_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal sourcePendingAfter = BigDecimal.ZERO;

    @Column(name = "destination_pending_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal destinationPendingBefore = BigDecimal.ZERO;

    @Column(name = "destination_pending_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal destinationPendingAfter = BigDecimal.ZERO;

    @Column(nullable = false)
    private int sequence;

    @Version
    private long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public SiteTransfer getTransfer() { return transfer; }
    public void setTransfer(SiteTransfer transfer) { this.transfer = transfer; }
    public AgreementItem getSourceAgreementItem() { return sourceAgreementItem; }
    public void setSourceAgreementItem(AgreementItem sourceAgreementItem) { this.sourceAgreementItem = sourceAgreementItem; }
    public AgreementItem getDestinationAgreementItem() { return destinationAgreementItem; }
    public void setDestinationAgreementItem(AgreementItem destinationAgreementItem) { this.destinationAgreementItem = destinationAgreementItem; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public String getItemCodeSnapshot() { return itemCodeSnapshot; }
    public void setItemCodeSnapshot(String itemCodeSnapshot) { this.itemCodeSnapshot = itemCodeSnapshot; }
    public String getItemNameSnapshot() { return itemNameSnapshot; }
    public void setItemNameSnapshot(String itemNameSnapshot) { this.itemNameSnapshot = itemNameSnapshot; }
    public String getDescriptionSnapshot() { return descriptionSnapshot; }
    public void setDescriptionSnapshot(String descriptionSnapshot) { this.descriptionSnapshot = descriptionSnapshot; }
    public String getSizeSnapshot() { return sizeSnapshot; }
    public void setSizeSnapshot(String sizeSnapshot) { this.sizeSnapshot = sizeSnapshot; }
    public String getUnitSnapshot() { return unitSnapshot; }
    public void setUnitSnapshot(String unitSnapshot) { this.unitSnapshot = unitSnapshot; }
    public BigDecimal getWeightPerPieceSnapshot() { return weightPerPieceSnapshot; }
    public void setWeightPerPieceSnapshot(BigDecimal weightPerPieceSnapshot) { this.weightPerPieceSnapshot = weightPerPieceSnapshot; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getTotalWeight() { return totalWeight; }
    public void setTotalWeight(BigDecimal totalWeight) { this.totalWeight = totalWeight; }
    public BigDecimal getSourcePendingBefore() { return sourcePendingBefore; }
    public void setSourcePendingBefore(BigDecimal sourcePendingBefore) { this.sourcePendingBefore = sourcePendingBefore; }
    public BigDecimal getSourcePendingAfter() { return sourcePendingAfter; }
    public void setSourcePendingAfter(BigDecimal sourcePendingAfter) { this.sourcePendingAfter = sourcePendingAfter; }
    public BigDecimal getDestinationPendingBefore() { return destinationPendingBefore; }
    public void setDestinationPendingBefore(BigDecimal destinationPendingBefore) { this.destinationPendingBefore = destinationPendingBefore; }
    public BigDecimal getDestinationPendingAfter() { return destinationPendingAfter; }
    public void setDestinationPendingAfter(BigDecimal destinationPendingAfter) { this.destinationPendingAfter = destinationPendingAfter; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
