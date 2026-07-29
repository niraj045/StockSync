package com.stocksync.inventory.service;

import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.inventory.dto.*;
import com.stocksync.inventory.entity.Item;
import com.stocksync.inventory.entity.ItemCategory;
import com.stocksync.inventory.repository.ItemCategoryRepository;
import com.stocksync.inventory.repository.ItemRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Locale;

@Service
public class MasterDataService {
    private final ItemCategoryRepository categoryRepository;
    private final ItemRepository itemRepository;

    public MasterDataService(ItemCategoryRepository categoryRepository, ItemRepository itemRepository) {
        this.categoryRepository = categoryRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "categories")
    public Page<CategoryResponse> categories(String search, Boolean active, Pageable pageable) {
        return categoryRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("name")), term),
                        cb.like(cb.lower(root.get("description")), term)));
            }
            if (active != null) predicates.add(cb.equal(root.get("active"), active));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(this::categoryResponse);
    }

    @Transactional
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new BusinessRuleException("CATEGORY_NAME_ALREADY_EXISTS", "Category name already exists");
        }
        ItemCategory category = new ItemCategory();
        apply(category, request);
        category.setCreatedBy(auditor());
        category.setUpdatedBy(auditor());
        return categoryResponse(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(cacheNames = "categories", allEntries = true)
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        ItemCategory category = category(id);
        checkVersion(category.getVersion(), request.version(), ItemCategory.class, id);
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new BusinessRuleException("CATEGORY_NAME_ALREADY_EXISTS", "Category name already exists");
        }
        if (!request.active() && itemRepository.existsByCategoryId(id)) {
            throw new BusinessRuleException("CATEGORY_IN_USE", "A category with items cannot be deactivated");
        }
        apply(category, request);
        category.setUpdatedBy(auditor());
        return categoryResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public Page<ItemResponse> items(String search, Long categoryId, Boolean active, Pageable pageable) {
        return itemRepository.findAll((root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (search != null && !search.isBlank()) {
                String term = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(cb.like(cb.lower(root.get("itemCode")), term),
                        cb.like(cb.lower(root.get("itemName")), term)));
            }
            if (categoryId != null) predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            if (active != null) predicates.add(cb.equal(root.get("active"), active));
            return cb.and(predicates.toArray(Predicate[]::new));
        }, pageable).map(this::itemResponse);
    }

    @Transactional(readOnly = true)
    public ItemResponse itemById(Long id) { return itemResponse(item(id)); }

    @Transactional
    public ItemResponse createItem(ItemRequest request) {
        if (itemRepository.existsByItemCodeIgnoreCase(request.itemCode().trim())) {
            throw new BusinessRuleException("ITEM_CODE_ALREADY_EXISTS", "Item code already exists");
        }
        Item item = new Item();
        apply(item, request);
        item.setCreatedBy(auditor());
        item.setUpdatedBy(auditor());
        return itemResponse(itemRepository.save(item));
    }

    @Transactional
    public ItemResponse updateItem(Long id, ItemRequest request) {
        Item item = item(id);
        checkVersion(item.getVersion(), request.version(), Item.class, id);
        if (itemRepository.existsByItemCodeIgnoreCaseAndIdNot(request.itemCode().trim(), id)) {
            throw new BusinessRuleException("ITEM_CODE_ALREADY_EXISTS", "Item code already exists");
        }
        apply(item, request);
        item.setUpdatedBy(auditor());
        return itemResponse(itemRepository.save(item));
    }

    private void apply(ItemCategory category, CategoryRequest request) {
        category.setName(request.name().trim());
        category.setDescription(trim(request.description()));
        category.setActive(request.active());
    }

    private void apply(Item item, ItemRequest request) {
        ItemCategory category = category(request.categoryId());
        if (!category.isActive()) throw new BusinessRuleException("CATEGORY_INACTIVE", "Item category is inactive");
        item.setItemCode(request.itemCode().trim().toUpperCase(Locale.ROOT));
        item.setItemName(request.itemName().trim());
        item.setCategory(category);
        item.setSize(trim(request.size()));
        item.setUnit(request.unit().trim());
        item.setWeightPerPiece(request.weightPerPiece());
        item.setPurchaseValue(request.purchaseValue());
        item.setRentalConfiguration(trim(request.rentalConfiguration()));
        item.setLossRate(request.lossRate());
        item.setScrapValue(request.scrapValue());
        item.setMinimumStock(request.minimumStock());
        item.setActive(request.active());
    }

    private ItemCategory category(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("CATEGORY_NOT_FOUND", "Category not found"));
    }
    private Item item(Long id) {
        return itemRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("ITEM_NOT_FOUND", "Item not found"));
    }
    private CategoryResponse categoryResponse(ItemCategory c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getDescription(), c.isActive(), c.getVersion());
    }
    private ItemResponse itemResponse(Item i) {
        return new ItemResponse(i.getId(), i.getItemCode(), i.getItemName(), i.getCategory().getId(),
                i.getCategory().getName(), i.getSize(), i.getUnit(), i.getWeightPerPiece(), i.getPurchaseValue(),
                i.getRentalConfiguration(), i.getLossRate(), i.getScrapValue(), i.getMinimumStock(),
                i.isActive(), i.getVersion());
    }
    private String auditor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "system" : authentication.getName();
    }
    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private void checkVersion(long actual, Long supplied, Class<?> type, Long id) {
        if (supplied == null || actual != supplied) throw new ObjectOptimisticLockingFailureException(type, id);
    }
}
