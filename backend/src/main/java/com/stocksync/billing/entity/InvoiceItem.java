package com.stocksync.billing.entity;

import com.stocksync.agreement.entity.AgreementItem;
import com.stocksync.inventory.entity.Item;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Column(name = "line_type", nullable = false, length = 30)
    private String lineType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_item_id")
    private AgreementItem agreementItem;

    @Column(name = "source_type", length = 50)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_document_number", length = 50)
    private String sourceDocumentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(name = "item_code_snapshot", length = 50)
    private String itemCodeSnapshot;

    @Column(name = "item_name_snapshot")
    private String itemNameSnapshot;

    @Column(name = "size_snapshot", length = 50)
    private String sizeSnapshot;

    @Column(name = "unit_snapshot", length = 20)
    private String unitSnapshot;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(precision = 19, scale = 4)
    private BigDecimal area;

    @Column(precision = 19, scale = 4)
    private BigDecimal weight;

    @Column(name = "billable_days")
    private Integer billableDays;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal rate = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean taxable = true;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }
    public String getLineType() { return lineType; }
    public void setLineType(String lineType) { this.lineType = lineType; }
    public AgreementItem getAgreementItem() { return agreementItem; }
    public void setAgreementItem(AgreementItem agreementItem) { this.agreementItem = agreementItem; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getSourceDocumentNumber() { return sourceDocumentNumber; }
    public void setSourceDocumentNumber(String sourceDocumentNumber) { this.sourceDocumentNumber = sourceDocumentNumber; }
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
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getArea() { return area; }
    public void setArea(BigDecimal area) { this.area = area; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public Integer getBillableDays() { return billableDays; }
    public void setBillableDays(Integer billableDays) { this.billableDays = billableDays; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public boolean isTaxable() { return taxable; }
    public void setTaxable(boolean taxable) { this.taxable = taxable; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public int getSequenceNumber() { return sequenceNumber; }
    public void setSequenceNumber(int sequenceNumber) { this.sequenceNumber = sequenceNumber; }
}
