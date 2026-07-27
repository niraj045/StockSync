package com.stocksync.exception.service;

import com.stocksync.agreement.entity.Agreement;
import com.stocksync.agreement.repository.AgreementRepository;
import com.stocksync.audit.service.UserActivityLogService;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.challan.entity.ReceivingChallan;
import com.stocksync.challan.entity.ReceivingChallanItem;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.common.numbering.DocumentNumberService;
import com.stocksync.common.numbering.DocumentType;
import com.stocksync.exception.dto.ItemExchangeRequest;
import com.stocksync.exception.dto.ItemExchangeResponse;
import com.stocksync.exception.entity.*;
import com.stocksync.exception.repository.ItemExchangeRepository;
import com.stocksync.inventory.entity.Item;
import com.stocksync.inventory.entity.StockBalance;
import com.stocksync.inventory.entity.StockTransaction;
import com.stocksync.inventory.entity.SiteStockBalance;
import com.stocksync.inventory.repository.ItemRepository;
import com.stocksync.inventory.repository.StockBalanceRepository;
import com.stocksync.inventory.repository.StockTransactionRepository;
import com.stocksync.inventory.repository.SiteStockBalanceRepository;
import com.stocksync.party.entity.Party;
import com.stocksync.party.repository.PartyRepository;
import com.stocksync.site.entity.Site;
import com.stocksync.site.repository.SiteRepository;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ItemExchangeService {

    private final ItemExchangeRepository exchanges;
    private final StockBalanceRepository globalBalances;
    private final SiteStockBalanceRepository siteBalances;
    private final StockTransactionRepository transactions;
    private final AgreementRepository agreements;
    private final PartyRepository parties;
    private final SiteRepository sites;
    private final ItemRepository items;
    private final UserRepository users;
    private final UserActivityLogService audit;
    private final DocumentNumberService numbering;
    private final JdbcTemplate jdbc;
    private final EntityManager em;

    public ItemExchangeService(
            ItemExchangeRepository exchanges,
            StockBalanceRepository globalBalances,
            SiteStockBalanceRepository siteBalances,
            StockTransactionRepository transactions,
            AgreementRepository agreements,
            PartyRepository parties,
            SiteRepository sites,
            ItemRepository items,
            UserRepository users,
            UserActivityLogService audit,
            DocumentNumberService numbering,
            JdbcTemplate jdbc,
            EntityManager em) {
        this.exchanges = exchanges;
        this.globalBalances = globalBalances;
        this.siteBalances = siteBalances;
        this.transactions = transactions;
        this.agreements = agreements;
        this.parties = parties;
        this.sites = sites;
        this.items = items;
        this.users = users;
        this.audit = audit;
        this.numbering = numbering;
        this.jdbc = jdbc;
        this.em = em;
    }

    @Transactional
    public ItemExchangeResponse create(ItemExchangeRequest r, HttpServletRequest http) {
        if (r.expectedItemId().equals(r.actualItemId())) {
            throw new BusinessRuleException("SAME_ITEM_EXCHANGE", "Expected and actual items must be different");
        }

        Agreement ag = agreements.findById(r.agreementId()).orElseThrow();
        Party p = parties.findById(r.partyId()).orElseThrow();
        Site s = sites.findById(r.siteId()).orElseThrow();
        Item expected = items.findById(r.expectedItemId()).orElseThrow();
        Item actual = items.findById(r.actualItemId()).orElseThrow();

        if (!expected.getUnit().equalsIgnoreCase(actual.getUnit())) {
            throw new BusinessRuleException("INCOMPATIBLE_EXCHANGE_UNITS", "Expected unit '" + expected.getUnit() + "' is incompatible with actual unit '" + actual.getUnit() + "'");
        }

        BigDecimal expQty = Optional.ofNullable(r.expectedQuantity()).orElse(BigDecimal.ZERO);
        BigDecimal actQty = Optional.ofNullable(r.actualQuantity()).orElse(BigDecimal.ZERO);
        BigDecimal expWt = Optional.ofNullable(r.expectedWeight()).orElse(BigDecimal.ZERO);
        BigDecimal actWt = Optional.ofNullable(r.actualWeight()).orElse(BigDecimal.ZERO);

        if (expQty.compareTo(BigDecimal.ZERO) <= 0 || actQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("INVALID_QUANTITY", "Expected and actual quantities must be positive");
        }
        if (expWt.compareTo(BigDecimal.ZERO) < 0 || actWt.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("INVALID_WEIGHT", "Weights cannot be negative");
        }

        String destStatus = r.destinationStockStatus() != null ? r.destinationStockStatus().toUpperCase() : "AVAILABLE";
        if (!destStatus.equals("AVAILABLE") && !destStatus.equals("DAMAGED")) {
            throw new BusinessRuleException("INVALID_STATUS", "Destination stock status must be AVAILABLE or DAMAGED");
        }

        ItemExchange e = new ItemExchange();
        e.setSourceType(ExceptionSourceType.MANUAL_SITE_DECLARATION);
        e.setAgreement(ag);
        e.setParty(p);
        e.setSite(s);
        e.setExpectedItem(expected);
        e.setActualItem(actual);
        e.setExchangeDate(r.exchangeDate());
        e.setExpectedQuantity(expQty);
        e.setActualQuantity(actQty);
        e.setExpectedWeight(expWt);
        e.setActualWeight(actWt);
        e.setDestinationStockStatus(destStatus);
        e.setReason(r.reason());
        e.setStatus(ExchangeStatus.DRAFT);
        e.setExchangeNumber(numbering.next(DocumentType.ITEM_EXCHANGE, e.getExchangeDate()));

        e.setCreatedBy(auditor());
        e.setUpdatedBy(auditor());

        ItemExchange saved = exchanges.save(e);
        audit("ITEM_EXCHANGE_CREATED", "ItemExchange", saved.getId(), "Created manual exchange: " + saved.getExchangeNumber(), http);
        return response(saved);
    }

    @Transactional
    public ItemExchangeResponse update(Long id, ItemExchangeRequest r, HttpServletRequest http) {
        ItemExchange e = exchanges.findById(id).orElseThrow();
        if (e.getStatus() != ExchangeStatus.DRAFT) {
            throw new BusinessRuleException("INVALID_EXCHANGE_STATUS", "Only DRAFT records can be updated");
        }

        if (r.expectedItemId().equals(r.actualItemId())) {
            throw new BusinessRuleException("SAME_ITEM_EXCHANGE", "Expected and actual items must be different");
        }

        Item expected = items.findById(r.expectedItemId()).orElseThrow();
        Item actual = items.findById(r.actualItemId()).orElseThrow();

        if (!expected.getUnit().equalsIgnoreCase(actual.getUnit())) {
            throw new BusinessRuleException("INCOMPATIBLE_EXCHANGE_UNITS", "Expected unit '" + expected.getUnit() + "' is incompatible with actual unit '" + actual.getUnit() + "'");
        }

        BigDecimal expQty = Optional.ofNullable(r.expectedQuantity()).orElse(BigDecimal.ZERO);
        BigDecimal actQty = Optional.ofNullable(r.actualQuantity()).orElse(BigDecimal.ZERO);
        BigDecimal expWt = Optional.ofNullable(r.expectedWeight()).orElse(BigDecimal.ZERO);
        BigDecimal actWt = Optional.ofNullable(r.actualWeight()).orElse(BigDecimal.ZERO);

        if (expQty.compareTo(BigDecimal.ZERO) <= 0 || actQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("INVALID_QUANTITY", "Expected and actual quantities must be positive");
        }
        if (expWt.compareTo(BigDecimal.ZERO) < 0 || actWt.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("INVALID_WEIGHT", "Weights cannot be negative");
        }

        String destStatus = r.destinationStockStatus() != null ? r.destinationStockStatus().toUpperCase() : "AVAILABLE";
        if (!destStatus.equals("AVAILABLE") && !destStatus.equals("DAMAGED")) {
            throw new BusinessRuleException("INVALID_STATUS", "Destination stock status must be AVAILABLE or DAMAGED");
        }

        e.setExpectedItem(expected);
        e.setActualItem(actual);
        e.setExchangeDate(r.exchangeDate());
        e.setExpectedQuantity(expQty);
        e.setActualQuantity(actQty);
        e.setExpectedWeight(expWt);
        e.setActualWeight(actWt);
        e.setDestinationStockStatus(destStatus);
        e.setReason(r.reason());
        e.setUpdatedBy(auditor());

        ItemExchange saved = exchanges.save(e);
        audit("ITEM_EXCHANGE_UPDATED", "ItemExchange", saved.getId(), "Updated manual exchange: " + saved.getExchangeNumber(), http);
        return response(saved);
    }

    @Transactional
    public ItemExchangeResponse post(Long id, HttpServletRequest http) {
        ItemExchange e = exchanges.findByIdWithDetails(id).orElseThrow();
        if (e.getStatus() != ExchangeStatus.DRAFT) {
            return response(e); // Idempotency
        }

        Item expItem = e.getExpectedItem();
        Item actItem = e.getActualItem();
        Site site = e.getSite();
        BigDecimal expQty = e.getExpectedQuantity();
        BigDecimal actQty = e.getActualQuantity();

        // Lock global and site balances for expected item
        StockBalance expBalance = globalBalances.findForUpdate(expItem.getId()).orElseGet(() -> {
            jdbc.update("INSERT IGNORE INTO stock_balances(item_id) VALUES(?)", expItem.getId());
            return globalBalances.findForUpdate(expItem.getId()).orElseThrow();
        });

        SiteStockBalance expSiteBalance = siteBalances.findForUpdate(site.getId(), expItem.getId()).orElseGet(() -> {
            jdbc.update("INSERT IGNORE INTO site_stock_balances(site_id, item_id) VALUES(?, ?)", site.getId(), expItem.getId());
            return siteBalances.findForUpdate(site.getId(), expItem.getId()).orElseThrow();
        });

        if (expQty.compareTo(expSiteBalance.getPendingQuantity()) > 0) {
            throw new BusinessRuleException("INSUFFICIENT_SITE_STOCK", "Expected item exchange quantity " + expQty + " exceeds site pending stock of " + expSiteBalance.getPendingQuantity());
        }

        // Lock global and site balances for actual item
        StockBalance actBalance = globalBalances.findForUpdate(actItem.getId()).orElseGet(() -> {
            jdbc.update("INSERT IGNORE INTO stock_balances(item_id) VALUES(?)", actItem.getId());
            return globalBalances.findForUpdate(actItem.getId()).orElseThrow();
        });

        // 1. Process expected item return from site (without godown available change)
        expSiteBalance.setPendingQuantity(expSiteBalance.getPendingQuantity().subtract(expQty));
        siteBalances.save(expSiteBalance);

        expBalance.setIssuedQuantity(expBalance.getIssuedQuantity().subtract(expQty));
        globalBalances.save(expBalance);

        postLedger(expItem, "EXCHANGE", e.getExchangeDate(), expQty, "OUT", "PENDING_SITE", e.getId(), site, e.getParty());

        // 2. Process actual item entering godown bucket
        if ("DAMAGED".equals(e.getDestinationStockStatus())) {
            actBalance.setDamagedQuantity(actBalance.getDamagedQuantity().add(actQty));
            postLedger(actItem, "EXCHANGE", e.getExchangeDate(), actQty, "IN", "DAMAGED", e.getId(), site, e.getParty());
        } else {
            actBalance.setAvailableQuantity(actBalance.getAvailableQuantity().add(actQty));
            actBalance.setAvailableWeight(actBalance.getAvailableQuantity().multiply(Optional.ofNullable(actItem.getWeightPerPiece()).orElse(BigDecimal.ZERO)));
            postLedger(actItem, "EXCHANGE", e.getExchangeDate(), actQty, "IN", "AVAILABLE", e.getId(), site, e.getParty());
        }
        globalBalances.save(actBalance);

        e.setStatus(ExchangeStatus.POSTED);
        e.setPostedAt(Instant.now());
        e.setPostedBy(auditor());
        e.setUpdatedBy(auditor());

        ItemExchange saved = exchanges.save(e);
        audit("ITEM_EXCHANGE_POSTED", "ItemExchange", saved.getId(), "Posted manual exchange: " + saved.getExchangeNumber(), http);
        return response(saved);
    }

    @Transactional
    public ItemExchangeResponse cancel(Long id, String reason, HttpServletRequest http) {
        ItemExchange e = exchanges.findByIdWithDetails(id).orElseThrow();
        if (e.getStatus() == ExchangeStatus.CANCELLED) {
            return response(e); // Idempotency
        }
        if (e.getStatus() != ExchangeStatus.POSTED) {
            throw new BusinessRuleException("INVALID_EXCHANGE_STATUS", "Only POSTED records can be cancelled");
        }
        if (e.getSourceType() == ExceptionSourceType.RECEIVING_CHALLAN) {
            throw new BusinessRuleException("INVALID_REVERSAL_REQUEST", "Linked receiving challan exchange records cannot be cancelled independently");
        }
        if (reason == null || reason.trim().isBlank()) {
            throw new BusinessRuleException("REVERSAL_REASON_REQUIRED", "Cancellation reason is required");
        }

        Item expItem = e.getExpectedItem();
        Item actItem = e.getActualItem();
        Site site = e.getSite();
        BigDecimal expQty = e.getExpectedQuantity();
        BigDecimal actQty = e.getActualQuantity();

        StockBalance expBalance = globalBalances.findForUpdate(expItem.getId()).orElseThrow();
        SiteStockBalance expSiteBalance = siteBalances.findForUpdate(site.getId(), expItem.getId()).orElseThrow();

        StockBalance actBalance = globalBalances.findForUpdate(actItem.getId()).orElseThrow();

        // 1. Revert expected item
        expSiteBalance.setPendingQuantity(expSiteBalance.getPendingQuantity().add(expQty));
        siteBalances.save(expSiteBalance);

        expBalance.setIssuedQuantity(expBalance.getIssuedQuantity().add(expQty));
        globalBalances.save(expBalance);

        postLedger(expItem, "REVERSAL", e.getExchangeDate(), expQty, "IN", "PENDING_SITE", e.getId(), site, e.getParty());

        // 2. Revert actual item
        if ("DAMAGED".equals(e.getDestinationStockStatus())) {
            actBalance.setDamagedQuantity(actBalance.getDamagedQuantity().subtract(actQty));
            postLedger(actItem, "REVERSAL", e.getExchangeDate(), actQty, "OUT", "DAMAGED", e.getId(), site, e.getParty());
        } else {
            actBalance.setAvailableQuantity(actBalance.getAvailableQuantity().subtract(actQty));
            actBalance.setAvailableWeight(actBalance.getAvailableQuantity().multiply(Optional.ofNullable(actItem.getWeightPerPiece()).orElse(BigDecimal.ZERO)));
            postLedger(actItem, "REVERSAL", e.getExchangeDate(), actQty, "OUT", "AVAILABLE", e.getId(), site, e.getParty());
        }
        globalBalances.save(actBalance);

        e.setStatus(ExchangeStatus.CANCELLED);
        e.setCancelledAt(Instant.now());
        e.setCancelledBy(auditor());
        e.setCancellationReason(reason);
        e.setUpdatedBy(auditor());

        ItemExchange saved = exchanges.save(e);
        audit("ITEM_EXCHANGE_CANCELLED", "ItemExchange", saved.getId(), "Cancelled manual exchange: " + saved.getExchangeNumber() + ", Reason: " + reason, http);
        return response(saved);
    }

    @Transactional
    public void createReceivingLinked(ReceivingChallan c, ReceivingChallanItem item, HttpServletRequest http) {
        if (exchanges.existsBySourceReceivingChallanItemId(item.getId())) {
            return; // Prevent duplicates
        }

        ItemExchange e = new ItemExchange();
        e.setSourceType(ExceptionSourceType.RECEIVING_CHALLAN);
        e.setSourceReceivingChallan(c);
        e.setSourceReceivingChallanItem(item);
        e.setAgreement(c.getAgreement());
        e.setParty(c.getParty());
        e.setSite(c.getSite());
        e.setExpectedItem(item.getExchangedFromItem());
        e.setActualItem(item.getExchangedToItem());
        e.setExchangeDate(c.getReceiveDate());
        e.setExpectedQuantity(item.getExchangedQuantity());
        e.setActualQuantity(item.getExchangedQuantity());
        e.setExpectedWeight(item.getExchangedQuantity().multiply(Optional.ofNullable(item.getExchangedFromItem().getWeightPerPiece()).orElse(BigDecimal.ZERO)));
        e.setActualWeight(item.getExchangedQuantity().multiply(Optional.ofNullable(item.getExchangedToItem().getWeightPerPiece()).orElse(BigDecimal.ZERO)));
        e.setDestinationStockStatus("AVAILABLE");
        e.setReason("Originating from Receiving Challan " + c.getReceivingChallanNumber());
        e.setStatus(ExchangeStatus.POSTED);
        e.setPostedAt(Instant.now());
        e.setPostedBy(c.getPostedBy());
        e.setExchangeNumber(numbering.next(DocumentType.ITEM_EXCHANGE, e.getExchangeDate()));

        e.setCreatedBy(c.getCreatedBy());
        e.setUpdatedBy(c.getUpdatedBy());

        ItemExchange saved = exchanges.save(e);
        audit("ITEM_EXCHANGE_POSTED", "ItemExchange", saved.getId(), "Created receiving-linked exchange: " + saved.getExchangeNumber(), http);
    }

    @Transactional
    public void reverseReceivingLinked(Long challanItemId, String reason, HttpServletRequest http) {
        exchanges.findAll((root, query, cb) -> cb.equal(root.get("sourceReceivingChallanItem").get("id"), challanItemId))
                .stream().findFirst().ifPresent(e -> {
                    if (e.getStatus() != ExchangeStatus.CANCELLED) {
                        e.setStatus(ExchangeStatus.CANCELLED);
                        e.setCancelledAt(Instant.now());
                        e.setCancelledBy(auditor());
                        e.setCancellationReason(reason);
                        e.setUpdatedBy(auditor());
                        exchanges.save(e);
                        audit("ITEM_EXCHANGE_CANCELLED", "ItemExchange", e.getId(), "Cancelled receiving-linked exchange: " + e.getExchangeNumber() + ", Reason: " + reason, http);
                    }
                });
    }

    @Transactional(readOnly = true)
    public Page<ItemExchangeResponse> list(Specification<ItemExchange> spec, Pageable pageable) {
        return exchanges.findAll(spec, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public ItemExchangeResponse getById(Long id) {
        return exchanges.findByIdWithDetails(id).map(this::response).orElseThrow();
    }

    private void postLedger(Item item, String txType, LocalDate txDate, BigDecimal qty, String direction, String bucket, Long sourceId, Site site, Party party) {
        BigDecimal weight = qty.multiply(Optional.ofNullable(item.getWeightPerPiece()).orElse(BigDecimal.ZERO));
        StockTransaction tx = new StockTransaction();
        tx.setItem(item);
        tx.setTransactionType(txType);
        tx.setTransactionDate(txDate);
        tx.setQuantity(qty);
        tx.setWeight(weight);
        tx.setDirection(direction);
        tx.setStockBucket(bucket);
        tx.setSourceType("ITEM_EXCHANGE");
        tx.setSourceId(sourceId);
        tx.setSite(site);
        tx.setParty(party);
        tx.setCreatedBy(auditor());
        transactions.save(tx);
    }

    private User currentUser() {
        return users.findByUsernameIgnoreCase(auditor()).orElse(null);
    }

    private void audit(String action, String entity, long id, String description, HttpServletRequest request) {
        User u = currentUser();
        audit.log(u == null ? null : u.getId(), auditor(), action, entity, String.valueOf(id), description, request);
    }

    private String auditor() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "system" : a.getName();
    }

    private ItemExchangeResponse response(ItemExchange e) {
        return new ItemExchangeResponse(
                e.getId(),
                e.getExchangeNumber(),
                e.getSourceType().name(),
                e.getSourceReceivingChallan() == null ? null : e.getSourceReceivingChallan().getId(),
                e.getSourceReceivingChallan() == null ? null : e.getSourceReceivingChallan().getReceivingChallanNumber(),
                e.getSourceReceivingChallanItem() == null ? null : e.getSourceReceivingChallanItem().getId(),
                e.getAgreement() == null ? null : e.getAgreement().getId(),
                e.getAgreement() == null ? null : e.getAgreement().getAgreementNumber(),
                e.getParty().getId(),
                e.getParty().getLegalName(),
                e.getSite().getId(),
                e.getSite().getSiteName(),
                e.getExpectedItem().getId(),
                e.getExpectedItem().getItemCode(),
                e.getExpectedItem().getItemName(),
                e.getActualItem().getId(),
                e.getActualItem().getItemCode(),
                e.getActualItem().getItemName(),
                e.getExchangeDate(),
                e.getExpectedQuantity(),
                e.getActualQuantity(),
                e.getExpectedWeight(),
                e.getActualWeight(),
                e.getDestinationStockStatus(),
                e.getReason(),
                e.getStatus().name(),
                e.getPostedAt(),
                e.getPostedBy(),
                e.getCancelledAt(),
                e.getCancelledBy(),
                e.getCancellationReason(),
                e.getCreatedAt(),
                e.getCreatedBy(),
                e.getUpdatedAt(),
                e.getUpdatedBy(),
                e.getVersion()
        );
    }
}
