package com.stocksync.migration.dto;
import jakarta.validation.constraints.*;
public record ReverseImportRequest(@NotBlank @Size(max=500)String reason){}
