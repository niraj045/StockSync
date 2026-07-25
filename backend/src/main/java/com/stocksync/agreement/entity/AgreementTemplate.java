package com.stocksync.agreement.entity;

import com.stocksync.common.persistence.AuditedEntity;
import jakarta.persistence.*;

@Entity @Table(name = "agreement_templates")
public class AgreementTemplate extends AuditedEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,unique=true,length=150) private String name;
    @Column(length=500) private String description;
    @Column(name="original_filename",nullable=false,length=255) private String originalFilename;
    @Column(name="stored_filename",nullable=false,unique=true,length=255) private String storedFilename;
    @Column(name="content_type",nullable=false,length=100) private String contentType;
    @Column(name="file_size",nullable=false) private long fileSize;
    @Column(name="storage_path",nullable=false,length=500) private String storagePath;
    @Column(nullable=false) private boolean active=true;
    public Long getId(){return id;} public String getName(){return name;} public void setName(String v){name=v;} public String getDescription(){return description;}
    public void setDescription(String v){description=v;} public String getOriginalFilename(){return originalFilename;}
    public void setOriginalFilename(String v){originalFilename=v;} public String getStoredFilename(){return storedFilename;}
    public void setStoredFilename(String v){storedFilename=v;} public String getContentType(){return contentType;}
    public void setContentType(String v){contentType=v;} public long getFileSize(){return fileSize;} public void setFileSize(long v){fileSize=v;}
    public String getStoragePath(){return storagePath;} public void setStoragePath(String v){storagePath=v;}
    public boolean isActive(){return active;} public void setActive(boolean v){active=v;}
}
