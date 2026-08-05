package com.stocksync.site.controller;
import com.stocksync.site.dto.*;
import com.stocksync.site.entity.SiteStatus;
import com.stocksync.site.service.SiteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import com.stocksync.migration.service.LedgerImportService;
import com.stocksync.migration.parser.LedgerImportParser.LedgerItemTotal;
import java.util.List;

@RestController @RequestMapping("/api/v1/sites")
public class SiteController {
    private final SiteService service;
    private final LedgerImportService ledgerImportService;
    
    public SiteController(SiteService service, LedgerImportService ledgerImportService){
        this.service=service;
        this.ledgerImportService=ledgerImportService;
    }
    @GetMapping public Page<SiteResponse> list(@RequestParam(required=false)String search,@RequestParam(required=false)Long partyId,
            @RequestParam(required=false)SiteStatus status,@RequestParam(required=false)Boolean defaulter,Pageable pageable){
        return service.list(search,partyId,status,defaulter,pageable);}
    @GetMapping("/{id}") public SiteResponse get(@PathVariable Long id){return service.get(id);}
    @PostMapping @PreAuthorize("hasRole('ADMIN')") public SiteResponse create(@Valid @RequestBody SiteRequest r){return service.create(r);}
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public SiteResponse update(@PathVariable Long id,@Valid @RequestBody SiteRequest r){return service.update(id,r);}

    @PostMapping(value = "/{id}/import-ledger", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATIONS')")
    public ResponseEntity<List<LedgerItemTotal>> importLedger(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ledgerImportService.importLedger(id, file));
    }
}
