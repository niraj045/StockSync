package com.stocksync.challan.entity;

import com.stocksync.inventory.entity.Item;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "receiving_challan_items")
public class ReceivingChallanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiving_challan_id")
    private ReceivingChallan receivingChallan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_issued_challan_item_id")
    private IssuedChallanItem linkedIssuedChallanItem;

    @Column(name = "opening_import_transaction_id")
    private Long openingImportTransactionId;

    @Column(name = "item_code_snapshot", nullable = false, length = 50)
    private String itemCodeSnapshot;

    @Column(name = "item_name_snapshot", nullable = false)
    private String itemNameSnapshot;

    @Column(name = "size_snapshot", length = 50)
    private String sizeSnapshot;

    @Column(name = "unit_snapshot", nullable = false, length = 20)
    private String unitSnapshot;

    @Column(name = "pending_quantity_snapshot", nullable = false, precision = 19, scale = 4)
    private BigDecimal pendingQuantitySnapshot = BigDecimal.ZERO;

    @Column(name = "good_returned_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal goodReturnedQuantity = BigDecimal.ZERO;

    @Column(name = "damaged_returned_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal damagedReturnedQuantity = BigDecimal.ZERO;

    @Column(name = "lost_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal lostQuantity = BigDecimal.ZERO;

    @Column(name = "extra_returned_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal extraReturnedQuantity = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchanged_from_item_id")
    private Item exchangedFromItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchanged_to_item_id")
    private Item exchangedToItem;

    @Column(name = "exchanged_quantity", nullable = false, precision = 19, scale = 4)
    private BigDecimal exchangedQuantity = BigDecimal.ZERO;

    @Column(name = "weight_per_piece_snapshot", precision = 19, scale = 4)
    private BigDecimal weightPerPieceSnapshot;

    @Column(name = "good_returned_weight", nullable = false, precision = 19, scale = 4)
    private BigDecimal goodReturnedWeight = BigDecimal.ZERO;

    @Column(name = "damaged_weight", nullable = false, precision = 19, scale = 4)
    private BigDecimal damagedWeight = BigDecimal.ZERO;

    @Column(name = "lost_weight", nullable = false, precision = 19, scale = 4)
    private BigDecimal lostWeight = BigDecimal.ZERO;

    @Column(length = 255)
    private String notes;

    @Column(nullable = false)
    private int sequence;

    @Version
    private long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ReceivingChallan getReceivingChallan() { return receivingChallan; }
    public void setReceivingChallan(ReceivingChallan receivingChallan) { this.receivingChallan = receivingChallan; }
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public IssuedChallanItem getLinkedIssuedChallanItem() { return linkedIssuedChallanItem; }
    public void setLinkedIssuedChallanItem(IssuedChallanItem linkedIssuedChallanItem) { this.linkedIssuedChallanItem = linkedIssuedChallanItem; }
    public Long getOpeningImportTransactionId() { return openingImportTransactionId; }
    public void setOpeningImportTransactionId(Long openingImportTransactionId) { this.openingImportTransactionId = openingImportTransactionId; }
    public String getItemCodeSnapshot() { return itemCodeSnapshot; }
    public void setItemCodeSnapshot(String itemCodeSnapshot) { this.itemCodeSnapshot = itemCodeSnapshot; }
    public String getItemNameSnapshot() { return itemNameSnapshot; }
    public void setItemNameSnapshot(String itemNameSnapshot) { this.itemNameSnapshot = itemNameSnapshot; }
    public String getSizeSnapshot() { return sizeSnapshot; }
    public void setSizeSnapshot(String sizeSnapshot) { this.sizeSnapshot = sizeSnapshot; }
    public String getUnitSnapshot() { return unitSnapshot; }
    public void setUnitSnapshot(String unitSnapshot) { this.unitSnapshot = unitSnapshot; }
    public BigDecimal getPendingQuantitySnapshot() { return pendingQuantitySnapshot; }
    public void setPendingQuantitySnapshot(BigDecimal pendingQuantitySnapshot) { this.pendingQuantitySnapshot = pendingQuantitySnapshot; }
    public BigDecimal getGoodReturnedQuantity() { return goodReturnedQuantity; }
    public void setGoodReturnedQuantity(BigDecimal goodReturnedQuantity) { this.goodReturnedQuantity = goodReturnedQuantity; }
    public BigDecimal getDamagedReturnedQuantity() { return damagedReturnedQuantity; }
    public void setDamagedReturnedQuantity(BigDecimal damagedReturnedQuantity) { this.damagedReturnedQuantity = damagedReturnedQuantity; }
    public BigDecimal getLostQuantity() { return lostQuantity; }
    public void setLostQuantity(BigDecimal lostQuantity) { this.lostQuantity = lostQuantity; }
    public BigDecimal getExtraReturnedQuantity() { return extraReturnedQuantity; }
    public void setExtraReturnedQuantity(BigDecimal extraReturnedQuantity) { this.extraReturnedQuantity = extraReturnedQuantity; }
    public Item getExchangedFromItem() { return exchangedFromItem; }
    public void setExchangedFromItem(Item exchangedFromItem) { this.exchangedFromItem = exchangedFromItem; }
    public Item getExchangedToItem() { return exchangedToItem; }
    public void setExchangedToItem(Item exchangedToItem) { this.exchangedToItem = exchangedToItem; }
    public BigDecimal getExchangedQuantity() { return exchangedQuantity; }
    public void setExchangedQuantity(BigDecimal exchangedQuantity) { this.exchangedQuantity = exchangedQuantity; }
    public BigDecimal getWeightPerPieceSnapshot() { return weightPerPieceSnapshot; }
    public void setWeightPerPieceSnapshot(BigDecimal weightPerPieceSnapshot) { this.weightPerPieceSnapshot = weightPerPieceSnapshot; }
    public BigDecimal getGoodReturnedWeight() { return goodReturnedWeight; }
    public void setGoodReturnedWeight(BigDecimal goodReturnedWeight) { this.goodReturnedWeight = goodReturnedWeight; }
    public BigDecimal getDamagedWeight() { return damagedWeight; }
    public void setDamagedWeight(BigDecimal damagedWeight) { this.damagedWeight = damagedWeight; }
    public BigDecimal getLostWeight() { return lostWeight; }
    public void setLostWeight(BigDecimal lostWeight) { this.lostWeight = lostWeight; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public int getSequence() { return sequence; }
    public void setSequence(int sequence) { this.sequence = sequence; }
    public long getVersion() { return version; }
    public void setVersion(long version) { this.version = version; }
}
