package com.stocksync.migration.service;

import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.inventory.entity.Item;
import com.stocksync.inventory.entity.ItemCategory;
import com.stocksync.inventory.repository.ItemCategoryRepository;
import com.stocksync.inventory.repository.ItemRepository;
import com.stocksync.inventory.service.OpeningStockAccess;
import com.stocksync.migration.parser.LedgerImportParser;
import com.stocksync.site.entity.Site;
import com.stocksync.site.entity.SiteStatus;
import com.stocksync.site.repository.SiteRepository;
import org.springframework.jdbc.core.JdbcTemplate;
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
    private final JdbcTemplate jdbc;

    public LedgerImportService(LedgerImportParser parser, ItemRepository itemRepository, 
                               ItemCategoryRepository categoryRepository, SiteRepository siteRepository, 
                               OpeningStockAccess openingStockAccess, JdbcTemplate jdbc) {
        this.parser = parser;
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
        this.siteRepository = siteRepository;
        this.openingStockAccess = openingStockAccess;
        this.jdbc = jdbc;
    }

    @Transactional
    public List<LedgerImportParser.LedgerItemTotal> importLedger(Long siteId, MultipartFile file) {
        Site selectedSite = siteRepository.findById(siteId)
                .orElseThrow(() -> new BusinessRuleException("SITE_NOT_FOUND", "Site not found"));
        
        LedgerImportParser.LedgerParseResult result;
        try (InputStream is = file.getInputStream()) {
            result = parser.parseWithSiteName(is);
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessRuleException("LEDGER_READ_FAILED", "Unable to read the ledger file: " + e.getMessage());
        }

        List<LedgerImportParser.LedgerItemTotal> totals = result.totals();
        if (totals.isEmpty()) {
            throw new BusinessRuleException("LEDGER_EMPTY", "No material totals found in the ledger");
        }
        
        // Auto-detect and resolve site
        Site targetSite = resolveTargetSite(selectedSite, result.detectedSiteName());

        String actor = SecurityContextHolder.getContext().getAuthentication() != null ? 
                SecurityContextHolder.getContext().getAuthentication().getName() : "system";

        List<LedgerImportParser.LedgerItemTotal> posted = new ArrayList<>();

        for (LedgerImportParser.LedgerItemTotal total : totals) {
            Item item = findOrCreateItem(total.itemName());

            // GUARD: Skip if an OPENING_SITE_BALANCE transaction already exists for this site+item.
            Integer existingCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_transactions WHERE item_id = ? AND site_id = ? AND transaction_type IN ('OPENING_SITE_BALANCE','OPENING_GODOWN_BALANCE')",
                Integer.class, item.getId(), targetSite.getId()
            );
            if (existingCount != null && existingCount > 0) {
                continue;
            }

            OpeningStockAccess.OpeningCommand command = new OpeningStockAccess.OpeningCommand(
                    null, null, item.getId(), targetSite.getParty().getId(), targetSite.getId(),
                    "OPENING_SITE_BALANCE", "ISSUED", LocalDate.now(), total.totalDelivered(),
                    "Imported from D&R Ledger", actor
            );
            openingStockAccess.post(command);
            posted.add(total);
        }

        return posted;
    }

    private Site resolveTargetSite(Site selectedSite, String detectedSiteName) {
        if (detectedSiteName == null || detectedSiteName.isBlank()) {
            return selectedSite;
        }
        
        String detected = detectedSiteName.trim();
        String partyName = selectedSite.getParty().getLegalName().trim();
        
        // Strip party name prefix from detected site name if present (e.g. "SBUT - A Wing..." -> "A Wing...")
        String cleanDetected = detected;
        if (cleanDetected.toLowerCase().startsWith(partyName.toLowerCase())) {
            cleanDetected = cleanDetected.substring(partyName.length()).trim();
            if (cleanDetected.startsWith("-") || cleanDetected.startsWith(":")) {
                cleanDetected = cleanDetected.substring(1).trim();
            }
        }
        if (cleanDetected.isBlank()) {
            cleanDetected = detected;
        }
        
        final String searchName = cleanDetected;
        List<Site> partySites = siteRepository.findByPartyId(selectedSite.getParty().getId());
        
        // 1. Try exact match (case insensitive) among party's sites for searchName or full detected
        Optional<Site> exactMatch = partySites.stream()
            .filter(s -> s.getSiteName().equalsIgnoreCase(searchName) || s.getSiteName().equalsIgnoreCase(detected))
            .findFirst();
        if (exactMatch.isPresent()) {
            return exactMatch.get();
        }
        
        // 2. Try partial match ONLY on existing sub-sites (excluding the generic site named identical to party)
        Optional<Site> subSiteMatch = partySites.stream()
            .filter(s -> !s.getSiteName().equalsIgnoreCase(partyName))
            .filter(s -> searchName.toLowerCase().contains(s.getSiteName().toLowerCase()) || 
                         s.getSiteName().toLowerCase().contains(searchName.toLowerCase()))
            .findFirst();
        if (subSiteMatch.isPresent()) {
            return subSiteMatch.get();
        }
        
        // 3. If no existing sub-site matches, create a new specific site under this party
        Site newSite = new Site();
        newSite.setParty(selectedSite.getParty());
        newSite.setSiteName(cleanDetected);
        
        String code = cleanDetected.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (code.length() > 20) code = code.substring(0, 20);
        if (code.isBlank()) code = "SITE";
        
        String finalCode = code;
        int counter = 1;
        while(siteRepository.existsBySiteCodeIgnoreCase(finalCode)) {
            finalCode = code + counter++;
        }
        
        newSite.setSiteCode(finalCode);
        newSite.setStatus(SiteStatus.ACTIVE);
        newSite.setDefaulter(false);
        newSite.setStartDate(LocalDate.now());
        newSite.setNotes("Auto-created from D&R import");
        
        return siteRepository.save(newSite);
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
        String code = itemName.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (code.length() > 20) code = code.substring(0, 20);
        newItem.setItemCode(code);
        newItem.setCategory(category);
        newItem.setUnit("NOS");
        newItem.setActive(true);

        return itemRepository.save(newItem);
    }
}
