package com.stocksync.agreement.dto;
import java.time.Instant;
public record AgreementTemplateResponse(Long id,String templateCode,String name,String description,String renderingMode,String layoutKey,
        boolean builtIn,int templateVersion,String analysisStatus,Integer pageCount,String originalFilename,String contentType,long fileSize,
        boolean active,Long version,Instant createdAt){}
