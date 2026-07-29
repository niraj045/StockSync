package com.stocksync.agreement.controller;

import com.stocksync.agreement.dto.AgreementTemplateResponse;
import com.stocksync.agreement.dto.AgreementTemplateAnalysisResponse;
import com.stocksync.agreement.service.AgreementTemplateService;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/agreement-templates")
public class AgreementTemplateController {
    private final AgreementTemplateService service;

    public AgreementTemplateController(AgreementTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public List<AgreementTemplateResponse> list() {
        return service.list();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public AgreementTemplateResponse upload(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestPart("file") MultipartFile file) {
        return service.upload(name, description, file);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        var d = service.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(d.filename(), StandardCharsets.UTF_8).build().toString())
                .body(d.resource());
    }

    @GetMapping("/{id}/analysis")
    @PreAuthorize("hasRole('ADMIN')")
    public AgreementTemplateAnalysisResponse analysis(@PathVariable Long id) {
        return service.analysis(id);
    }

    @PostMapping("/{id}/validate-analysis")
    @PreAuthorize("hasRole('ADMIN')")
    public AgreementTemplateResponse validateAnalysis(@PathVariable Long id) {
        return service.validateAnalysis(id);
    }
}
