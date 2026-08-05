package com.stocksync.migration.service;

import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.inventory.entity.Item;
import com.stocksync.inventory.entity.ItemCategory;
import com.stocksync.inventory.repository.ItemCategoryRepository;
import com.stocksync.inventory.repository.ItemRepository;
import com.stocksync.inventory.service.OpeningStockAccess;
import com.stocksync.migration.parser.LedgerImportParser;
import com.stocksync.site.entity.Site;
import com.stocksync.site.repository.SiteRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LedgerImportService {

    private final LedgerImportParser parser;
    private final ItemRepository itemRepository;
    private final ItemCategoryRepository categoryRepository;
    private final SiteRepository siteRepository;
    private final OpeningStockAccess openingStockAccess;

    public LedgerImportService(LedgerImportParser parser, ItemRepository itemRepository, 
                               ItemCategoryRepository categoryRepository, SiteRepository siteRepository, 
                               OpeningStockAccess openingStockAccess) {
        this.parser = parser;
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.siteRepository = siteRepository;
        this.openingStockAccess = openingStockAccess;
    }

    @Transactional
    public List<LedgerImportParser.LedgerItemTotal> importLedger(Long siteId, MultipartFile file) {
        Site site = siteRepository.findById(siteId)
                .orElseThrow(() -> new BusinessRuleException("SITE_NOT_FOUND", "Site not found"));
        
        List<LedgerImportParser.LedgerItemTotal> totals;
        try (InputStream is = file.getInputStream()) {
            totals = parser.parse(is);
        } catch (Exception e) {
            throw new BusinessRuleException("LEDGER_READ_FAILED", "Unable to read the ledger file: " + e.getMessage());
        }

        if (totals.isEmpty()) {
            throw new BusinessRuleException("LEDGER_EMPTY", "No material totals found in the ledger");
        }

        String actor = SecurityContextHolder.getContext().getAuthentication() != null ? 
                SecurityContextHolder.getContext().getAuthentication().getName() : "system";

        for (LedgerImportParser.LedgerItemTotal total : totals) {
            Item item = findOrCreateItem(total.itemName());

            OpeningStockAccess.OpeningCommand command = new OpeningStockAccess.OpeningCommand(
                    null, null, item.getId(), site.getParty().getId(), site.getId(),
                    "OPENING_SITE_BALANCE", "ISSUED", LocalDate.now(), total.totalDelivered(),
                    "Imported from D&R Ledger", actor
            );
            openingStockAccess.post(command);
        }

        return totals;
    }

    private Item findOrCreateItem(String itemName) {
        Optional<Item> existing = itemRepository.findByItemNameIgnoreCase(itemName);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new item under a generic category
        ItemCategory category = categoryRepository.findByNameIgnoreCase("Imported Items")
                .orElseGet(() -> {
                    ItemCategory newCategory = new ItemCategory();
                    newCategory.setName("Imported Items");
                    newCategory.setDescription("Auto-created for imported ledgers");
                    return categoryRepository.save(newCategory);
                });

        Item newItem = new Item();
        newItem.setItemName(itemName);
        newItem.setItemCode(itemName.replaceAll("[^a-zA-Z0-9]", "").toUpperCase());
        if (newItem.getItemCode().length() > 20) {
            newItem.setItemCode(newItem.getItemCode().substring(0, 20));
        }
        newItem.setCategory(category);
        newItem.setUnit("Nos");
        newItem.setActive(true);

        return itemRepository.save(newItem);
    }
}
