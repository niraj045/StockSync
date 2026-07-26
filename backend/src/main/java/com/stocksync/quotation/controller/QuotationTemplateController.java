package com.stocksync.quotation.controller;

import com.stocksync.quotation.dto.*;
import com.stocksync.quotation.service.QuotationTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/quotation-templates")
public class QuotationTemplateController {
    private final QuotationTemplateService service;
    public QuotationTemplateController(QuotationTemplateService service){this.service=service;}
    @GetMapping public Page<QuotationTemplateResponse> list(@RequestParam(required=false)String search,
            @RequestParam(required=false)Boolean active,Pageable pageable){return service.list(search,active,pageable);}
    @GetMapping("/{id}") public QuotationTemplateResponse get(@PathVariable Long id){return service.get(id);}
    @PostMapping @PreAuthorize("hasRole('ADMIN')")
    public QuotationTemplateResponse create(@Valid @RequestBody QuotationTemplateRequest body,HttpServletRequest req){return service.create(body,req);}
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public QuotationTemplateResponse update(@PathVariable Long id,@Valid @RequestBody QuotationTemplateRequest body,HttpServletRequest req){return service.update(id,body,req);}
    @PostMapping("/{id}/activate") @PreAuthorize("hasRole('ADMIN')")
    public QuotationTemplateResponse activate(@PathVariable Long id,HttpServletRequest req){return service.active(id,true,req);}
    @PostMapping("/{id}/deactivate") @PreAuthorize("hasRole('ADMIN')")
    public QuotationTemplateResponse deactivate(@PathVariable Long id,HttpServletRequest req){return service.active(id,false,req);}
}
