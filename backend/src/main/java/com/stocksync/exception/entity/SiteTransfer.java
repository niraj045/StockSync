package com.stocksync.exception.entity;

import com.stocksync.agreement.entity.Agreement;
import com.stocksync.common.persistence.AuditedEntity;
import com.stocksync.party.entity.Party;
import com.stocksync.site.entity.Site;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "site_transfers")
public class SiteTransfer extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_number", nullable = false, unique = true, length = 50)
    private String transferNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_agreement_id")
    private Agreement sourceAgreement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_agreement_id")
    private Agreement destinationAgreement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_party_id")
    private Party sourceParty;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_site_id")
    private Site sourceSite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_party_id")
    private Party destinationParty;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destination_site_id")
    private Site destinationSite;

    @Column(name = "transfer_date", nullable = false)
    private LocalDate transferDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TransferStatus status = TransferStatus.DRAFT;

    @Column(name = "vehicle_number", length = 50)
    private String vehicleNumber;

    @Column(name = "driver_name", length = 100)
    private String driverName;

    @Column(name = "driver_phone", length = 20)
    private String driverPhone;

    @Column(name = "transporter_id")
    private Long transporterId;

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

    @OneToMany(mappedBy = "transfer", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequence ASC")
    private List<SiteTransferItem> items = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTransferNumber() { return transferNumber; }
    public void setTransferNumber(String transferNumber) { this.transferNumber = transferNumber; }
    public Agreement getSourceAgreement() { return sourceAgreement; }
    public void setSourceAgreement(Agreement sourceAgreement) { this.sourceAgreement = sourceAgreement; }
    public Agreement getDestinationAgreement() { return destinationAgreement; }
    public void setDestinationAgreement(Agreement destinationAgreement) { this.destinationAgreement = destinationAgreement; }
    public Party getSourceParty() { return sourceParty; }
    public void setSourceParty(Party sourceParty) { this.sourceParty = sourceParty; }
    public Site getSourceSite() { return sourceSite; }
    public void setSourceSite(Site sourceSite) { this.sourceSite = sourceSite; }
    public Party getDestinationParty() { return destinationParty; }
    public void setDestinationParty(Party destinationParty) { this.destinationParty = destinationParty; }
    public Site getDestinationSite() { return destinationSite; }
    public void setDestinationSite(Site destinationSite) { this.destinationSite = destinationSite; }
    public LocalDate getTransferDate() { return transferDate; }
    public void setTransferDate(LocalDate transferDate) { this.transferDate = transferDate; }
    public TransferStatus getStatus() { return status; }
    public void setStatus(TransferStatus status) { this.status = status; }
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String vehicleNumber) { this.vehicleNumber = vehicleNumber; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getDriverPhone() { return driverPhone; }
    public void setDriverPhone(String driverPhone) { this.driverPhone = driverPhone; }
    public Long getTransporterId() { return transporterId; }
    public void setTransporterId(Long transporterId) { this.transporterId = transporterId; }
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
    public List<SiteTransferItem> getItems() { return items; }
    public void setItems(List<SiteTransferItem> items) { this.items = items; }

    public void addItem(SiteTransferItem item) {
        item.setTransfer(this);
        this.items.add(item);
    }
}
