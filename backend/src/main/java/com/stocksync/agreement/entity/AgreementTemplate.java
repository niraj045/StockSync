package com.stocksync.agreement.entity;

import com.stocksync.common.persistence.AuditedEntity;
import jakarta.persistence.*;

@Entity @Table(name = "agreement_templates")
public class AgreementTemplate extends AuditedEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="template_code",unique=true,length=80) private String templateCode;
    @Column(nullable=false,unique=true,length=150) private String name;
    @Column(length=500) private String description;
    @Enumerated(EnumType.STRING) @Column(name="rendering_mode",nullable=false,length=20)
    private AgreementTemplateRenderingMode renderingMode=AgreementTemplateRenderingMode.REFERENCE;
    @Column(name="layout_key",length=100) private String layoutKey;
    @Column(name="built_in",nullable=false) private boolean builtIn;
    @Column(name="template_version",nullable=false) private int templateVersion=1;
    @Enumerated(EnumType.STRING) @Column(name="analysis_status",nullable=false,length=30)
    private AgreementTemplateAnalysisStatus analysisStatus=AgreementTemplateAnalysisStatus.NOT_ANALYZED;
    @Column(name="page_count") private Integer pageCount;
    @Column(name="checksum_sha256",length=64) private String checksumSha256;
    @Lob @Column(name="extracted_text",columnDefinition="LONGTEXT") private String extractedText;
    @Lob @Column(name="detected_fields_json",columnDefinition="LONGTEXT") private String detectedFieldsJson;
    @Column(name="original_filename",length=255) private String originalFilename;
    @Column(name="stored_filename",unique=true,length=255) private String storedFilename;
    @Column(name="content_type",length=100) private String contentType;
    @Column(name="file_size",nullable=false) private long fileSize;
    @Column(name="storage_path",nullable=false,length=500) private String storagePath;
    @Column(nullable=false) private boolean active=true;
    public Long getId(){return id;} public String getTemplateCode(){return templateCode;} public void setTemplateCode(String v){templateCode=v;}
    public String getName(){return name;} public void setName(String v){name=v;} public String getDescription(){return description;}
    public void setDescription(String v){description=v;} public String getOriginalFilename(){return originalFilename;}
    public void setOriginalFilename(String v){originalFilename=v;} public String getStoredFilename(){return storedFilename;}
    public void setStoredFilename(String v){storedFilename=v;} public String getContentType(){return contentType;}
    public void setContentType(String v){contentType=v;} public long getFileSize(){return fileSize;} public void setFileSize(long v){fileSize=v;}
    public String getStoragePath(){return storagePath;} public void setStoragePath(String v){storagePath=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
    public AgreementTemplateRenderingMode getRenderingMode(){return renderingMode;} public void setRenderingMode(AgreementTemplateRenderingMode v){renderingMode=v;}
    public String getLayoutKey(){return layoutKey;} public void setLayoutKey(String v){layoutKey=v;}
    public boolean isBuiltIn(){return builtIn;} public void setBuiltIn(boolean v){builtIn=v;}
    public int getTemplateVersion(){return templateVersion;} public void setTemplateVersion(int v){templateVersion=v;}
    public AgreementTemplateAnalysisStatus getAnalysisStatus(){return analysisStatus;} public void setAnalysisStatus(AgreementTemplateAnalysisStatus v){analysisStatus=v;}
    public Integer getPageCount(){return pageCount;} public void setPageCount(Integer v){pageCount=v;}
    public String getChecksumSha256(){return checksumSha256;} public void setChecksumSha256(String v){checksumSha256=v;}
    public String getExtractedText(){return extractedText;} public void setExtractedText(String v){extractedText=v;}
    public String getDetectedFieldsJson(){return detectedFieldsJson;} public void setDetectedFieldsJson(String v){detectedFieldsJson=v;}
}
