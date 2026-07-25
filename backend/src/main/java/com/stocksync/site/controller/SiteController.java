package com.stocksync.site.controller;
import com.stocksync.site.dto.*;
import com.stocksync.site.entity.SiteStatus;
import com.stocksync.site.service.SiteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/sites")
public class SiteController {
    private final SiteService service;public SiteController(SiteService service){this.service=service;}
    @GetMapping public Page<SiteResponse> list(@RequestParam(required=false)String search,@RequestParam(required=false)Long partyId,
            @RequestParam(required=false)SiteStatus status,@RequestParam(required=false)Boolean defaulter,Pageable pageable){
        return service.list(search,partyId,status,defaulter,pageable);}
    @GetMapping("/{id}") public SiteResponse get(@PathVariable Long id){return service.get(id);}
    @PostMapping @PreAuthorize("hasRole('ADMIN')") public SiteResponse create(@Valid @RequestBody SiteRequest r){return service.create(r);}
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public SiteResponse update(@PathVariable Long id,@Valid @RequestBody SiteRequest r){return service.update(id,r);}
}
