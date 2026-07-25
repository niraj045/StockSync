package com.stocksync.party.controller;
import com.stocksync.party.dto.*;
import com.stocksync.party.service.PartyService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/parties")
public class PartyController {
    private final PartyService service; public PartyController(PartyService service){this.service=service;}
    @GetMapping public Page<PartyResponse> list(@RequestParam(required=false)String search,
            @RequestParam(required=false)Boolean active,Pageable pageable){return service.parties(search,active,pageable);}
    @GetMapping("/{id}") public PartyResponse get(@PathVariable Long id){return service.party(id);}
    @PostMapping @PreAuthorize("hasRole('ADMIN')") public PartyResponse create(@Valid @RequestBody PartyRequest r){return service.create(r);}
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')") public PartyResponse update(@PathVariable Long id,@Valid @RequestBody PartyRequest r){return service.update(id,r);}
}
