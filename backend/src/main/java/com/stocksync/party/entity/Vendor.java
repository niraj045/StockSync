package com.stocksync.party.entity;

import com.stocksync.common.persistence.AuditedEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "vendors")
public class Vendor extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150) private String name;
    @Column(length = 15) private String gstin;
    @Column(name = "contact_person", length = 100) private String contactPerson;
    @Column(length = 20) private String phone;
    @Column(length = 150) private String email;
    @Column(length = 500) private String address;
    @Column(length = 1000) private String notes;
    @Column(nullable = false) private boolean active = true;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String v) { name = v; }
    public String getGstin() { return gstin; }
    public void setGstin(String v) { gstin = v; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String v) { contactPerson = v; }
    public String getPhone() { return phone; }
    public void setPhone(String v) { phone = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { email = v; }
    public String getAddress() { return address; }
    public void setAddress(String v) { address = v; }
    public String getNotes() { return notes; }
    public void setNotes(String v) { notes = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { active = v; }
}
