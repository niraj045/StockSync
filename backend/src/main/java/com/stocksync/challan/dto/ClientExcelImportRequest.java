package com.stocksync.challan.dto;

import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotNull;

public record ClientExcelImportRequest(
    @NotNull MultipartFile file
) {}
