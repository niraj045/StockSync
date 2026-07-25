package com.stocksync.party.controller;
import com.stocksync.party.dto.*;
import com.stocksync.party.service.PartyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/vendors")
public class VendorController {
    private final PartyService service; public VendorController(PartyService service){this.service=service;}
    @GetMapping public Page<VendorResponse> list(@RequestParam(required=false)String search,
            @RequestParam(required=false)Boolean active,Pageable pageable){return service.vendors(search,active,pageable);}
    @PostMapping @PreAuthorize("hasRole('ADMIN')") public VendorResponse create(@Valid @RequestBody VendorRequest r){return service.createVendor(r);}
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public VendorResponse update(@PathVariable Long id,@Valid @RequestBody VendorRequest r){return service.updateVendor(id,r);}
}
