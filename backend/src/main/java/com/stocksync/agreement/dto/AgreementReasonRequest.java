package com.stocksync.agreement.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record AgreementReasonRequest(@NotBlank @Size(max=1000) String reason){}
