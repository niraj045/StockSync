package com.stocksync.quotation.controller;

import com.stocksync.quotation.dto.*;
import com.stocksync.quotation.entity.QuotationStatus;
import com.stocksync.quotation.service.QuotationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/quotations")
public class QuotationController {
    private final QuotationService service; public QuotationController(QuotationService service){this.service=service;}
    @GetMapping public Page<QuotationResponse> list(@RequestParam(required=false)String search,@RequestParam(required=false)QuotationStatus status,
            @RequestParam(required=false)Long partyId,@RequestParam(required=false)Long siteId,Pageable pageable){
        return service.list(search,status,partyId,siteId,pageable);}
    @GetMapping("/{id}") public QuotationResponse get(@PathVariable Long id){return service.get(id);}
    @PostMapping @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public QuotationResponse create(@Valid @RequestBody QuotationRequest body,HttpServletRequest request){return service.create(body,request);}
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public QuotationResponse update(@PathVariable Long id,@Valid @RequestBody QuotationRequest body,HttpServletRequest request){return service.update(id,body,request);}
    @PostMapping("/{id}/clone") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public QuotationResponse clone(@PathVariable Long id,HttpServletRequest request){return service.cloneQuotation(id,request);}
    @PostMapping("/{id}/send") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public QuotationResponse send(@PathVariable Long id,HttpServletRequest request){return service.transition(id,QuotationStatus.SENT,request);}
    @PostMapping("/{id}/approve") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public QuotationResponse approve(@PathVariable Long id,HttpServletRequest request){return service.transition(id,QuotationStatus.APPROVED,request);}
    @PostMapping("/{id}/reject") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public QuotationResponse reject(@PathVariable Long id,HttpServletRequest request){return service.transition(id,QuotationStatus.REJECTED,request);}
    @PostMapping("/{id}/expire") @PreAuthorize("hasAnyRole('ADMIN','OPERATIONS')")
    public QuotationResponse expire(@PathVariable Long id,HttpServletRequest request){return service.transition(id,QuotationStatus.EXPIRED,request);}
}
