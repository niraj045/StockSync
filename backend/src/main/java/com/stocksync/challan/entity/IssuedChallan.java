package com.stocksync.challan.entity;

import jakarta.persistence.*;
import com.stocksync.order.entity.SiteOrder;
import java.time.LocalDate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "issued_challans")
public class IssuedChallan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "challan_number", nullable = false, unique = true, length = 50)
    private String challanNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_order_id")
    private SiteOrder siteOrder;

    @Column(name = "dispatch_date", nullable = false)
    private LocalDate dispatchDate;

    @Column(name = "vehicle_number", length = 50)
    private String vehicleNumber;

    @Column(name = "driver_name", length = 100)
    private String driverName;

    @Column(length = 1000)
    private String notes;

    @Column(name = "created_by", nullable = false, length = 50)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "issuedChallan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<IssuedChallanItem> items = new ArrayList<>();

    @PrePersist
    void create() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getChallanNumber() { return challanNumber; }
    public void setChallanNumber(String v) { challanNumber = v; }
    public SiteOrder getSiteOrder() { return siteOrder; }
    public void setSiteOrder(SiteOrder v) { siteOrder = v; }
    public LocalDate getDispatchDate() { return dispatchDate; }
    public void setDispatchDate(LocalDate v) { dispatchDate = v; }
    public String getVehicleNumber() { return vehicleNumber; }
    public void setVehicleNumber(String v) { vehicleNumber = v; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String v) { driverName = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String v) { createdBy = v; }
    public Instant getCreatedAt() { return createdAt; }
    public List<IssuedChallanItem> getItems() { return items; }
    public void setItems(List<IssuedChallanItem> v) { items = v; }
}
