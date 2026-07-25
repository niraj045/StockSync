package com.stocksync.agreement.dto;
import java.time.Instant;
public record AgreementTemplateResponse(Long id,String name,String description,String originalFilename,String contentType,long fileSize,
        boolean active,Long version,Instant createdAt){}
