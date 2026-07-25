package com.stocksync.inventory.controller;

import com.stocksync.inventory.dto.ItemRequest;
import com.stocksync.inventory.dto.ItemResponse;
import com.stocksync.inventory.service.MasterDataService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/items")
public class ItemController {
    private final MasterDataService service;
    public ItemController(MasterDataService service) { this.service = service; }

    @GetMapping
    public Page<ItemResponse> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active, Pageable pageable) {
        return service.items(search, categoryId, active, pageable);
    }
    @GetMapping("/{id}")
    public ItemResponse get(@PathVariable Long id) { return service.itemById(id); }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ItemResponse create(@Valid @RequestBody ItemRequest request) { return service.createItem(request); }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ItemResponse update(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
        return service.updateItem(id, request);
    }
}
