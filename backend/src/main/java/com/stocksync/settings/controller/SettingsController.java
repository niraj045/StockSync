package com.stocksync.settings.controller;

import com.stocksync.settings.dto.DefaultTermsResponse;
import com.stocksync.settings.service.TermsTemplateService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

    private final TermsTemplateService termsTemplateService;

    public SettingsController(TermsTemplateService termsTemplateService) {
        this.termsTemplateService = termsTemplateService;
    }

    @GetMapping("/default-terms")
    public DefaultTermsResponse getDefaultTerms(@RequestParam(defaultValue = "AGREEMENT") String documentType) {
        return termsTemplateService.getDefaultTerms(documentType.toUpperCase());
    }
}
