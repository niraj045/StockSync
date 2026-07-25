package com.stocksync.file.dto;
import java.time.Instant;
public record FileAttachmentResponse(Long id,String entityType,Long entityId,String documentType,
        String originalFilename,String contentType,long fileSize,String description,String uploadedBy,Instant createdAt){}
