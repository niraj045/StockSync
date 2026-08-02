package com.stocksync.quotation.controller;

import com.stocksync.quotation.dto.*;
import com.stocksync.quotation.entity.QuotationStatus;
import com.stocksync.quotation.service.QuotationService;
import com.stocksync.quotation.service.QuotationPdfService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import java.time.LocalDate;

@RestController @RequestMapping("/api/v1/quotations")
public class QuotationController {
    private final QuotationService service; private final QuotationPdfService pdf;
    private final com.stocksync.quotation.service.SteelFabExactHireDocumentService exactDocuments;
    public QuotationController(QuotationService service,QuotationPdfService pdf,com.stocksync.quotation.service.SteelFabExactHireDocumentService exactDocuments){this.service=service;this.pdf=pdf;this.exactDocuments=exactDocuments;}
    @GetMapping public Page<QuotationResponse> list(@RequestParam(required=false)String search,@RequestParam(required=false)QuotationStatus status,
            @RequestParam(required=false)Long partyId,@RequestParam(required=false)Long siteId,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate quotationDateFrom,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate quotationDateTo,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate validUntilFrom,
            @RequestParam(required=false)@DateTimeFormat(iso=DateTimeFormat.ISO.DATE)LocalDate validUntilTo,Pageable pageable){
        return service.list(search,status,partyId,siteId,quotationDateFrom,quotationDateTo,validUntilFrom,validUntilTo,pageable);}
    @GetMapping("/{id}") public QuotationResponse get(@PathVariable Long id){return service.get(id);}
    @PostMapping @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public QuotationResponse create(@Valid @RequestBody QuotationRequest body,HttpServletRequest request){return service.create(body,request);}
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public QuotationResponse update(@PathVariable Long id,@Valid @RequestBody QuotationRequest body,HttpServletRequest request){return service.update(id,body,request);}
    @PostMapping("/{id}/clone") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public QuotationResponse clone(@PathVariable Long id,HttpServletRequest request){return service.cloneQuotation(id,request);}
    @PostMapping("/{id}/send") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public QuotationResponse send(@PathVariable Long id,HttpServletRequest request){return service.transition(id,QuotationStatus.SENT,null,request);}
    @PostMapping("/{id}/approve") @PreAuthorize("hasRole('ADMIN')")
    public QuotationResponse approve(@PathVariable Long id,HttpServletRequest request){return service.transition(id,QuotationStatus.APPROVED,null,request);}
    @PostMapping("/{id}/reject") @PreAuthorize("hasRole('ADMIN')")
    public QuotationResponse reject(@PathVariable Long id,@Valid @RequestBody ReasonRequest body,HttpServletRequest request){return service.transition(id,QuotationStatus.REJECTED,body.reason(),request);}
    @PostMapping("/{id}/cancel") @PreAuthorize("hasRole('ADMIN')")
    public QuotationResponse cancel(@PathVariable Long id,@Valid @RequestBody ReasonRequest body,HttpServletRequest request){return service.transition(id,QuotationStatus.CANCELLED,body.reason(),request);}
    @GetMapping("/{id}/pdf") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS','VIEWER')")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id,HttpServletRequest request){
        var document=pdf.generate(id);service.pdfGenerated(id,request);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+document.filename()+"\"").body(document.content());
    }
    @GetMapping("/{id}/pdf/preview") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS','ACCOUNTS','VIEWER')")
    public ResponseEntity<byte[]> exactPreview(@PathVariable Long id,@RequestParam(defaultValue="STEELFAB_EXACT_HIRE_V1")String template){
        if(!com.stocksync.quotation.service.SteelFabExactHirePdfService.TEMPLATE_CODE.equals(template))return ResponseEntity.badRequest().build();
        var document=exactDocuments.preview(id);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\""+document.filename()+"\"").body(document.content());
    }
    @PostMapping("/{id}/pdf/finalize") @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> finalizeExactPdf(@PathVariable Long id,HttpServletRequest request){
        var document=exactDocuments.finalizeDocument(id,request);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+document.filename()+"\"").body(document.content());
    }
}
