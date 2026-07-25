package com.stocksync.party.entity;

import com.stocksync.common.persistence.AuditedEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "parties")
public class Party extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "legal_name", nullable = false, length = 150) private String legalName;
    @Column(name = "trade_name", length = 150) private String tradeName;
    @Column(length = 15) private String gstin;
    @Column(length = 10) private String pan;
    @Column(name = "contact_person", length = 100) private String contactPerson;
    @Column(length = 20) private String phone;
    @Column(length = 150) private String email;
    @Column(length = 500) private String address;
    @Column(length = 100) private String state;
    @Column(length = 1000) private String notes;
    @Column(nullable = false) private boolean active = true;

    public Long getId() { return id; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String v) { legalName = v; }
    public String getTradeName() { return tradeName; }
    public void setTradeName(String v) { tradeName = v; }
    public String getGstin() { return gstin; }
    public void setGstin(String v) { gstin = v; }
    public String getPan() { return pan; }
    public void setPan(String v) { pan = v; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String v) { contactPerson = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public String getAddress() { return address; }
    public void setAddress(String v) { address = v; }
    public String getState() { return state; }
    public void setState(String v) { state = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { active = v; }
}
