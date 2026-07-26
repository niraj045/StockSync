package com.stocksync.migration.dto;
import jakarta.validation.constraints.*;
public record PostImportRequest(@AssertTrue(message="Explicit posting confirmation is required")boolean confirmed,
 @NotBlank @Pattern(regexp="[0-9a-fA-F]{64}")String expectedChecksum){}
