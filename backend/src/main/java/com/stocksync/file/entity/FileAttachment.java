package com.stocksync.file.entity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="file_attachments")
public class FileAttachment {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="entity_type",nullable=false,length=30) private String entityType;
    @Column(name="entity_id",nullable=false) private Long entityId;
    @Column(name="document_type",nullable=false,length=50) private String documentType;
    @Column(name="original_filename",nullable=false) private String originalFilename;
    @Column(name="stored_filename",nullable=false) private String storedFilename;
    @Column(name="content_type",nullable=false,length=100) private String contentType;
    @Column(name="file_size",nullable=false) private long fileSize;
    @Column(name="storage_path",nullable=false,length=500) private String storagePath;
    @Column(length=500) private String description;
    @Column(name="uploaded_by",nullable=false,length=50) private String uploadedBy;
    @Column(name="created_at",nullable=false,updatable=false) private Instant createdAt;
    @PrePersist void create(){createdAt=Instant.now();}
    public Long getId(){return id;} public String getEntityType(){return entityType;} public void setEntityType(String v){entityType=v;}
    public Long getEntityId(){return entityId;} public void setEntityId(Long v){entityId=v;}
    public String getDocumentType(){return documentType;} public void setDocumentType(String v){documentType=v;}
    public String getOriginalFilename(){return originalFilename;} public void setOriginalFilename(String v){originalFilename=v;}
    public String getStoredFilename(){return storedFilename;} public void setStoredFilename(String v){storedFilename=v;}
    public String getContentType(){return contentType;} public void setContentType(String v){contentType=v;}
    public long getFileSize(){return fileSize;} public void setFileSize(long v){fileSize=v;}
    public String getStoragePath(){return storagePath;} public void setStoragePath(String v){storagePath=v;}
    public String getDescription(){return description;} public void setDescription(String v){description=v;}
    public String getUploadedBy(){return uploadedBy;} public void setUploadedBy(String v){uploadedBy=v;}
    public Instant getCreatedAt(){return createdAt;}
}
