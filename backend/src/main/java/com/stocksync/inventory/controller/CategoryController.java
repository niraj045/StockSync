package com.stocksync.inventory.controller;

import com.stocksync.inventory.dto.CategoryRequest;
import com.stocksync.inventory.dto.CategoryResponse;
import com.stocksync.inventory.service.MasterDataService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    private final MasterDataService service;
    public CategoryController(MasterDataService service) { this.service = service; }

    @GetMapping
    public Page<CategoryResponse> list(@RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active, Pageable pageable) {
        return service.categories(search, active, pageable);
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return service.createCategory(request);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return service.updateCategory(id, request);
    }
}
