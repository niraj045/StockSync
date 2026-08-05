package com.stocksync.quotation.entity;

import com.stocksync.common.persistence.AuditedEntity;
import com.stocksync.file.entity.FileAttachment;
import jakarta.persistence.*;

@Entity
@Table(name = "quotation_templates")
public class QuotationTemplate extends AuditedEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "template_code", nullable = false, unique = true, length = 50) private String templateCode;
    @Column(nullable = false, length = 150) private String name;
    @Column(length = 500) private String description;
    @Column(name = "company_name", length = 200) private String companyName;
    @Column(name = "company_address", length = 1000) private String companyAddress;
    @Column(name = "company_gstin", length = 15) private String companyGstin;
    @Column(name = "header_text", length = 2000) private String headerText;
    @Column(name = "footer_text", length = 2000) private String footerText;
    @Column(name = "default_terms", length = 4000) private String defaultTerms;
    @Column(name = "default_notes", length = 2000) private String defaultNotes;
    @Column(name = "default_part_a_title", length = 255) private String defaultPartATitle;
    @Column(name = "default_part_a_text", columnDefinition = "TEXT") private String defaultPartAText;
    @Column(name = "default_part_b_title", length = 255) private String defaultPartBTitle;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "logo_attachment_id") private FileAttachment logoAttachment;
    @Column(nullable = false) private boolean active = true;

    public Long getId(){return id;}
    public String getTemplateCode(){return templateCode;} public void setTemplateCode(String v){templateCode=v;}
    public String getName(){return name;} public void setName(String v){name=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getCompanyName(){return companyName;} public void setCompanyName(String v){companyName=v;}
    public String getCompanyAddress(){return companyAddress;} public void setCompanyAddress(String v){companyAddress=v;}
    public String getCompanyGstin(){return companyGstin;} public void setCompanyGstin(String v){companyGstin=v;}
    public String getHeaderText(){return headerText;} public void setHeaderText(String v){headerText=v;}
    public String getFooterText(){return footerText;} public void setFooterText(String v){footerText=v;}
    public String getDefaultTerms(){return defaultTerms;} public void setDefaultTerms(String v){defaultTerms=v;}
    public String getDefaultNotes(){return defaultNotes;} public void setDefaultNotes(String v){defaultNotes=v;}
    public String getDefaultPartATitle(){return defaultPartATitle;} public void setDefaultPartATitle(String v){defaultPartATitle=v;}
    public String getDefaultPartAText(){return defaultPartAText;} public void setDefaultPartAText(String v){defaultPartAText=v;}
    public String getDefaultPartBTitle(){return defaultPartBTitle;} public void setDefaultPartBTitle(String v){defaultPartBTitle=v;}
    public FileAttachment getLogoAttachment(){return logoAttachment;} public void setLogoAttachment(FileAttachment v){logoAttachment=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}

