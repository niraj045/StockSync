package com.stocksync.challan.entity;

import com.stocksync.agreement.entity.Agreement;
import com.stocksync.common.persistence.AuditedEntity;
import com.stocksync.party.entity.Party;
import com.stocksync.site.entity.Site;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "receiving_challans")
public class ReceivingChallan extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receiving_challan_number", nullable = false, unique = true, length = 50)
    private String receivingChallanNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id")
    private Agreement agreement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "party_id")
    private Party party;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id")
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_issued_challan_id")
    private IssuedChallan linkedIssuedChallan;

    @Column(name = "receive_date", nullable = false)
    private LocalDate receiveDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReceivingStatus status = ReceivingStatus.DRAFT;

    @Column(name = "vehicle_number", length = 50)
    private String vehicleNumber;

    @Column(name = "driver_name", length = 100)
    private String driverName;

    @Column(name = "driver_phone", length = 20)
    private String driverPhone;

    @Column(name = "transporter_id")
    private Long transporterId;

    @Column(name = "transport_charge", nullable = false, precision = 19, scale = 2)
    private BigDecimal transportCharge = BigDecimal.ZERO;

    @Column(name = "handling_charge", nullable = false, precision = 19, scale = 2)
    private BigDecimal handlingCharge = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private SourceType sourceType;

    @Column(length = 1000)
    private String notes;

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

    @OneToMany(mappedBy = "receivingChallan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<ReceivingChallanItem> items = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReceivingChallanNumber() { return receivingChallanNumber; }
    public void setReceivingChallanNumber(String receivingChallanNumber) { this.receivingChallanNumber = receivingChallanNumber; }
    public Agreement getAgreement() { return agreement; }
    public void setAgreement(Agreement agreement) { this.agreement = agreement; }
    public Party getParty() { return party; }
    public void setParty(Party party) { this.party = party; }
    public Site getSite() { return site; }
    public void setSite(Site site) { this.site = site; }
    public IssuedChallan getLinkedIssuedChallan() { return linkedIssuedChallan; }
    public void setLinkedIssuedChallan(IssuedChallan linkedIssuedChallan) { this.linkedIssuedChallan = linkedIssuedChallan; }
    public LocalDate getReceiveDate() { return receiveDate; }
    public void setReceiveDate(LocalDate receiveDate) { this.receiveDate = receiveDate; }
    public ReceivingStatus getStatus() { return status; }
    public void setStatus(ReceivingStatus status) { this.status = status; }
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }
    public Long getTransporterId() { return transporterId; }
    public void setTransporterId(Long transporterId) { this.transporterId = transporterId; }
    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
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
    public List<ReceivingChallanItem> getItems() { return items; }
    public void setItems(List<ReceivingChallanItem> items) { this.items = items; }
    
    public void addItem(ReceivingChallanItem item) {
        item.setReceivingChallan(this);
        this.items.add(item);
    }
    public BigDecimal getTransportCharge() { return transportCharge; }
    public void setTransportCharge(BigDecimal transportCharge) { this.transportCharge = transportCharge; }
    public BigDecimal getHandlingCharge() { return handlingCharge; }
    public void setHandlingCharge(BigDecimal handlingCharge) { this.handlingCharge = handlingCharge; }
}
