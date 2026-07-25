package com.stocksync.site.entity;
import com.stocksync.common.persistence.AuditedEntity;
import com.stocksync.party.entity.Party;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name="sites")
public class Site extends AuditedEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="party_id") private Party party;
    @Column(name="site_name",nullable=false,length=150) private String siteName;
    @Column(name="site_code",nullable=false,length=50) private String siteCode;
    @Column(length=500) private String address;
    @Column(name="contact_person",length=100) private String contactPerson;
    @Column(name="start_date") private LocalDate startDate;
    @Column(name="expected_end_date") private LocalDate expectedEndDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private SiteStatus status;
    @Column(nullable=false) private boolean defaulter;
    @Column(name="closed_date") private LocalDate closedDate;
    @Column(length=1000) private String notes;
    public Long getId(){return id;} public Party getParty(){return party;} public void setParty(Party v){party=v;}
    public String getSiteName(){return siteName;} public void setSiteName(String v){siteName=v;}
    public String getSiteCode(){return siteCode;} public void setSiteCode(String v){siteCode=v;}
    public String getAddress(){return address;} public void setAddress(String v){address=v;}
    public String getContactPerson(){return contactPerson;} public void setContactPerson(String v){contactPerson=v;}
    public LocalDate getStartDate(){return startDate;} public void setStartDate(LocalDate v){startDate=v;}
    public LocalDate getExpectedEndDate(){return expectedEndDate;} public void setExpectedEndDate(LocalDate v){expectedEndDate=v;}
    public SiteStatus getStatus(){return status;} public void setStatus(SiteStatus v){status=v;}
    public boolean isDefaulter(){return defaulter;} public void setDefaulter(boolean v){defaulter=v;}
    public LocalDate getClosedDate(){return closedDate;} public void setClosedDate(LocalDate v){closedDate=v;}
    public String getNotes(){return notes;} public void setNotes(String v){notes=v;}
}
