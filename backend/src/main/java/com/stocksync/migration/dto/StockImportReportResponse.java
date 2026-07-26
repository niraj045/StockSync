package com.stocksync.migration.dto;
import java.util.List;
public record StockImportReportResponse(StockImportBatchResponse batch,StockImportPreviewResponse totals,
 List<LocationMappingResponse>locations,List<String>dataQualityFindings,List<String>knownClientQuestions){}
