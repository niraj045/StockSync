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
import com.stocksync.exception.dto.StockDamageRequest;
import com.stocksync.exception.dto.StockDamageResponse;
import com.stocksync.exception.entity.*;
import com.stocksync.exception.repository.StockDamageRepository;
import com.stocksync.file.entity.FileAttachment;
import com.stocksync.file.repository.FileAttachmentRepository;
import com.stocksync.inventory.entity.Item;
import com.stocksync.agreement.entity.AgreementItem;
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
public class StockDamageService {

    private final StockDamageRepository damages;
    private final StockBalanceRepository globalBalances;
    private final SiteStockBalanceRepository siteBalances;
    private final StockTransactionRepository transactions;
    private final AgreementRepository agreements;
    private final PartyRepository parties;
    private final SiteRepository sites;
    private final ItemRepository items;
    private final FileAttachmentRepository attachments;
    private final UserRepository users;
    private final UserActivityLogService audit;
    private final DocumentNumberService numbering;
    private final JdbcTemplate jdbc;
    private final EntityManager em;

    public StockDamageService(
            StockDamageRepository damages,
            StockBalanceRepository globalBalances,
            SiteStockBalanceRepository siteBalances,
            StockTransactionRepository transactions,
            AgreementRepository agreements,
            PartyRepository parties,
            SiteRepository sites,
            ItemRepository items,
            FileAttachmentRepository attachments,
            UserRepository users,
            UserActivityLogService audit,
            DocumentNumberService numbering,
            JdbcTemplate jdbc,
            EntityManager em) {
        this.damages = damages;
        this.globalBalances = globalBalances;
        this.siteBalances = siteBalances;
        this.transactions = transactions;
        this.agreements = agreements;
        this.parties = parties;
        this.sites = sites;
        this.items = items;
        this.attachments = attachments;
        this.users = users;
        this.audit = audit;
        this.numbering = numbering;
        this.jdbc = jdbc;
        this.em = em;
    }

    @Transactional
    public StockDamageResponse create(StockDamageRequest r, HttpServletRequest http) {
        Agreement ag = agreements.findById(r.agreementId()).orElseThrow();
        Party p = parties.findById(r.partyId()).orElseThrow();
        Site s = sites.findById(r.siteId()).orElseThrow();
        Item item = items.findById(r.itemId()).orElseThrow();

        BigDecimal qty = Optional.ofNullable(r.quantity()).orElse(BigDecimal.ZERO);
        BigDecimal wt = Optional.ofNullable(r.weight()).orElse(BigDecimal.ZERO);
        if (qty.compareTo(BigDecimal.ZERO) < 0 || wt.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("INVALID_QUANTITY", "Quantity and weight cannot be negative");
        }
        if (qty.compareTo(BigDecimal.ZERO) == 0 && wt.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleException("INVALID_QUANTITY", "At least quantity or weight must be positive");
        }

        BigDecimal rate = Optional.ofNullable(r.damageRate()).orElse(BigDecimal.ZERO);
        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("INVALID_RATE", "Damage rate cannot be negative");
        }

        BigDecimal estRepair = Optional.ofNullable(r.estimatedRepairCost()).orElse(BigDecimal.ZERO);
        if (estRepair.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("INVALID_COST", "Repair cost cannot be negative");
        }

        StockDamage d = new StockDamage();
        d.setSourceType(ExceptionSourceType.MANUAL_SITE_DECLARATION);
        d.setAgreement(ag);
        d.setParty(p);
        d.setSite(s);
        d.setItem(item);
        d.setDamageDate(r.damageDate());
        d.setQuantity(qty);
        d.setWeight(wt);
        d.setRepairable(r.repairable());
        d.setDamageType(DamageType.valueOf(r.damageType()));
        d.setConditionNotes(r.conditionNotes());
        d.setChargeMethod(ChargeMethod.valueOf(r.chargeMethod()));
        d.setDamageRate(rate);
        d.setCalculatedDamageAmount(calculateAmount(d.getChargeMethod(), qty, wt, rate));
        d.setEstimatedRepairCost(estRepair);
        d.setActualRepairCost(BigDecimal.ZERO);
        d.setAttachment(r.attachmentId() == null ? null : attachments.findById(r.attachmentId()).orElse(null));
        d.setStatus(DamageStatus.DRAFT);
        d.setDamageNumber(numbering.next(DocumentType.STOCK_DAMAGE, d.getDamageDate()));

        d.setCreatedBy(auditor());
        d.setUpdatedBy(auditor());

        StockDamage saved = damages.save(d);
        audit("STOCK_DAMAGE_CREATED", "StockDamage", saved.getId(), "Created manual damage: " + saved.getDamageNumber(), http);
        return response(saved);
    }

    @Transactional
    public StockDamageResponse update(Long id, StockDamageRequest r, HttpServletRequest http) {
        StockDamage d = damages.findById(id).orElseThrow();
        if (d.getStatus() != DamageStatus.DRAFT) {
            throw new BusinessRuleException("INVALID_DAMAGE_STATUS", "Only DRAFT records can be updated");
        }

        BigDecimal qty = Optional.ofNullable(r.quantity()).orElse(BigDecimal.ZERO);
        BigDecimal wt = Optional.ofNullable(r.weight()).orElse(BigDecimal.ZERO);
        if (qty.compareTo(BigDecimal.ZERO) < 0 || wt.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("INVALID_QUANTITY", "Quantity and weight cannot be negative");
        }
        if (qty.compareTo(BigDecimal.ZERO) == 0 && wt.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleException("INVALID_QUANTITY", "At least quantity or weight must be positive");
        }

        BigDecimal rate = Optional.ofNullable(r.damageRate()).orElse(BigDecimal.ZERO);
        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("INVALID_RATE", "Damage rate cannot be negative");
        }

        BigDecimal estRepair = Optional.ofNullable(r.estimatedRepairCost()).orElse(BigDecimal.ZERO);
        if (estRepair.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("INVALID_COST", "Repair cost cannot be negative");
        }

        d.setDamageDate(r.damageDate());
        d.setQuantity(qty);
        d.setWeight(wt);
        d.setRepairable(r.repairable());
        d.setDamageType(DamageType.valueOf(r.damageType()));
        d.setConditionNotes(r.conditionNotes());
        d.setChargeMethod(ChargeMethod.valueOf(r.chargeMethod()));
        d.setDamageRate(rate);
        d.setCalculatedDamageAmount(calculateAmount(d.getChargeMethod(), qty, wt, rate));
        d.setEstimatedRepairCost(estRepair);
        d.setAttachment(r.attachmentId() == null ? null : attachments.findById(r.attachmentId()).orElse(null));
        d.setUpdatedBy(auditor());

        StockDamage saved = damages.save(d);
        audit("STOCK_DAMAGE_UPDATED", "StockDamage", saved.getId(), "Updated manual damage: " + saved.getDamageNumber(), http);
        return response(saved);
    }

    @Transactional
    public StockDamageResponse recordDamage(Long id, HttpServletRequest http) {
        StockDamage d = damages.findByIdWithDetails(id).orElseThrow();
        if (d.getStatus() != DamageStatus.DRAFT) {
            return response(d); // Idempotency
        }

        Item item = d.getItem();
        Site site = d.getSite();
        BigDecimal qty = d.getQuantity();

        StockBalance balance = globalBalances.findForUpdate(item.getId()).orElseGet(() -> {
            jdbc.update("INSERT IGNORE INTO stock_balances(item_id) VALUES(?)", item.getId());
            return globalBalances.findForUpdate(item.getId()).orElseThrow();
        });

        SiteStockBalance siteBalance = siteBalances.findForUpdate(site.getId(), item.getId()).orElseGet(() -> {
            jdbc.update("INSERT IGNORE INTO site_stock_balances(site_id, item_id) VALUES(?, ?)", site.getId(), item.getId());
            return siteBalances.findForUpdate(site.getId(), item.getId()).orElseThrow();
        });

        if (qty.compareTo(siteBalance.getPendingQuantity()) > 0) {
            throw new BusinessRuleException("INSUFFICIENT_SITE_STOCK", "Damage quantity " + qty + " exceeds site pending stock of " + siteBalance.getPendingQuantity());
        }

        // Apply manual damage movements
        siteBalance.setPendingQuantity(siteBalance.getPendingQuantity().subtract(qty));
        siteBalances.save(siteBalance);

        balance.setDamagedQuantity(balance.getDamagedQuantity().add(qty));
        globalBalances.save(balance);

        // Ledger postings
        postLedger(item, "DAMAGE", d.getDamageDate(), qty, "OUT", "PENDING_SITE", d.getId(), site, d.getParty());
        postLedger(item, "DAMAGE", d.getDamageDate(), qty, "IN", "DAMAGED", d.getId(), site, d.getParty());

        d.setStatus(DamageStatus.RECORDED);
        d.setRecordedAt(Instant.now());
        d.setRecordedBy(auditor());
        d.setUpdatedBy(auditor());

        StockDamage saved = damages.save(d);
        audit("STOCK_DAMAGE_RECORDED", "StockDamage", saved.getId(), "Recorded manual damage: " + saved.getDamageNumber(), http);
        return response(saved);
    }

    @Transactional
    public StockDamageResponse startRepair(Long id, HttpServletRequest http) {
        StockDamage d = damages.findById(id).orElseThrow();
        if (d.getStatus() == DamageStatus.UNDER_REPAIR) {
            return response(d); // Idempotency
        }
        if (d.getStatus() != DamageStatus.RECORDED) {
            throw new BusinessRuleException("INVALID_DAMAGE_TRANSITION", "Only RECORDED records can start repair");
        }
        if (!d.isRepairable()) {
            throw new BusinessRuleException("INVALID_DAMAGE_TRANSITION", "Only repairable damages can start repair");
        }

        d.setStatus(DamageStatus.UNDER_REPAIR);
        d.setRepairStartedAt(Instant.now());
        d.setRepairStartedBy(auditor());
        d.setUpdatedBy(auditor());

        StockDamage saved = damages.save(d);
        audit("STOCK_DAMAGE_REPAIR_STARTED", "StockDamage", saved.getId(), "Started repair for: " + saved.getDamageNumber(), http);
        return response(saved);
    }

    @Transactional
    public StockDamageResponse markRepaired(Long id, BigDecimal actualCost, HttpServletRequest http) {
        StockDamage d = damages.findByIdWithDetails(id).orElseThrow();
        if (d.getStatus() == DamageStatus.REPAIRED) {
            return response(d); // Idempotency
        }
        if (d.getStatus() != DamageStatus.UNDER_REPAIR) {
            throw new BusinessRuleException("INVALID_DAMAGE_TRANSITION", "Only UNDER_REPAIR records can be marked as repaired");
        }

        BigDecimal cost = Optional.ofNullable(actualCost).orElse(BigDecimal.ZERO);
        if (cost.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("INVALID_COST", "Repair cost cannot be negative");
        }

        Item item = d.getItem();
        BigDecimal qty = d.getQuantity();

        StockBalance balance = globalBalances.findForUpdate(item.getId()).orElseThrow();

        // Move DAMAGED to AVAILABLE
        balance.setDamagedQuantity(balance.getDamagedQuantity().subtract(qty));
        balance.setAvailableQuantity(balance.getAvailableQuantity().add(qty));
        balance.setAvailableWeight(balance.getAvailableQuantity().multiply(Optional.ofNullable(item.getWeightPerPiece()).orElse(BigDecimal.ZERO)));
        globalBalances.save(balance);

        // Ledger postings
        postLedger(item, "REPAIR", LocalDate.now(), qty, "OUT", "DAMAGED", d.getId(), d.getSite(), d.getParty());
        postLedger(item, "REPAIR", LocalDate.now(), qty, "IN", "AVAILABLE", d.getId(), d.getSite(), d.getParty());

        d.setStatus(DamageStatus.REPAIRED);
        d.setActualRepairCost(cost);
        d.setRepairedAt(Instant.now());
        d.setRepairedBy(auditor());
        d.setUpdatedBy(auditor());

        StockDamage saved = damages.save(d);
        audit("STOCK_DAMAGE_REPAIRED", "StockDamage", saved.getId(), "Repaired stock damage: " + saved.getDamageNumber(), http);
        return response(saved);
    }

    @Transactional
    public StockDamageResponse scrap(Long id, HttpServletRequest http) {
        StockDamage d = damages.findByIdWithDetails(id).orElseThrow();
        if (d.getStatus() == DamageStatus.SCRAPPED) {
            return response(d); // Idempotency
        }
        if (d.getStatus() != DamageStatus.RECORDED && d.getStatus() != DamageStatus.UNDER_REPAIR) {
            throw new BusinessRuleException("INVALID_DAMAGE_TRANSITION", "Only RECORDED or UNDER_REPAIR records can be scrapped");
        }

        Item item = d.getItem();
        BigDecimal qty = d.getQuantity();

        StockBalance balance = globalBalances.findForUpdate(item.getId()).orElseThrow();

        // Move DAMAGED to SCRAPPED
        balance.setDamagedQuantity(balance.getDamagedQuantity().subtract(qty));
        balance.setScrappedQuantity(balance.getScrappedQuantity().add(qty));
        globalBalances.save(balance);

        // Ledger postings
        postLedger(item, "SCRAP", LocalDate.now(), qty, "OUT", "DAMAGED", d.getId(), d.getSite(), d.getParty());
        postLedger(item, "SCRAP", LocalDate.now(), qty, "IN", "SCRAPPED", d.getId(), d.getSite(), d.getParty());

        d.setStatus(DamageStatus.SCRAPPED);
        d.setScrappedAt(Instant.now());
        d.setScrappedBy(auditor());
        d.setUpdatedBy(auditor());

        StockDamage saved = damages.save(d);
        audit("STOCK_DAMAGE_SCRAPPED", "StockDamage", saved.getId(), "Scrapped stock damage: " + saved.getDamageNumber(), http);
        return response(saved);
    }

    @Transactional
    public StockDamageResponse reverse(Long id, String reason, HttpServletRequest http) {
        StockDamage d = damages.findByIdWithDetails(id).orElseThrow();
        if (d.getStatus() == DamageStatus.REVERSED) {
            return response(d); // Idempotency
        }
        if (d.getStatus() != DamageStatus.RECORDED && d.getStatus() != DamageStatus.UNDER_REPAIR) {
            throw new BusinessRuleException("INVALID_DAMAGE_TRANSITION", "Only RECORDED or UNDER_REPAIR records can be reversed");
        }
        if (d.getSourceType() == ExceptionSourceType.RECEIVING_CHALLAN) {
            throw new BusinessRuleException("INVALID_REVERSAL_REQUEST", "Linked receiving challan damage records cannot be reversed independently");
        }
        if (reason == null || reason.trim().isBlank()) {
            throw new BusinessRuleException("REVERSAL_REASON_REQUIRED", "Reversal reason is required");
        }

        Item item = d.getItem();
        Site site = d.getSite();
        BigDecimal qty = d.getQuantity();

        StockBalance balance = globalBalances.findForUpdate(item.getId()).orElseThrow();
        SiteStockBalance siteBalance = siteBalances.findForUpdate(site.getId(), item.getId()).orElseThrow();

        // Restore/revert quantities
        siteBalance.setPendingQuantity(siteBalance.getPendingQuantity().add(qty));
        siteBalances.save(siteBalance);

        balance.setDamagedQuantity(balance.getDamagedQuantity().subtract(qty));
        globalBalances.save(balance);

        // Compensating ledger entries (directions inverted)
        postLedger(item, "REVERSAL", d.getDamageDate(), qty, "IN", "PENDING_SITE", d.getId(), site, d.getParty());
        postLedger(item, "REVERSAL", d.getDamageDate(), qty, "OUT", "DAMAGED", d.getId(), site, d.getParty());

        d.setStatus(DamageStatus.REVERSED);
        d.setReversedAt(Instant.now());
        d.setReversedBy(auditor());
        d.setReversalReason(reason);
        d.setUpdatedBy(auditor());

        StockDamage saved = damages.save(d);
        audit("STOCK_DAMAGE_REVERSED", "StockDamage", saved.getId(), "Reversed stock damage: " + saved.getDamageNumber() + ", Reason: " + reason, http);
        return response(saved);
    }

    @Transactional
    public void createReceivingLinked(ReceivingChallan c, ReceivingChallanItem item, HttpServletRequest http) {
        if (damages.existsBySourceReceivingChallanItemId(item.getId())) {
            return; // Prevent duplicates
        }

        StockDamage d = new StockDamage();
        d.setSourceType(ExceptionSourceType.RECEIVING_CHALLAN);
        d.setSourceReceivingChallan(c);
        d.setSourceReceivingChallanItem(item);
        d.setAgreement(c.getAgreement());
        d.setParty(c.getParty());
        d.setSite(c.getSite());
        d.setItem(item.getItem());
        d.setDamageDate(c.getReceiveDate());
        d.setQuantity(item.getDamagedReturnedQuantity());
        d.setWeight(item.getDamagedWeight());
        d.setRepairable(true);
        d.setDamageType(DamageType.REPAIRABLE);
        d.setConditionNotes("Originating from Receiving Challan " + c.getReceivingChallanNumber());

        BigDecimal rate = BigDecimal.ZERO;
        ChargeMethod method = ChargeMethod.NONE;
        if (c.getAgreement() != null) {
            rate = c.getAgreement().getItems().stream()
                    .filter(ai -> ai.getItem().getId().equals(item.getItem().getId()))
                    .findFirst()
                    .map(AgreementItem::getDamageRate)
                    .orElse(BigDecimal.ZERO);
            if (rate.compareTo(BigDecimal.ZERO) > 0) {
                method = ChargeMethod.PER_PIECE;
            }
        }
        d.setChargeMethod(method);
        d.setDamageRate(rate);
        d.setCalculatedDamageAmount(calculateAmount(method, d.getQuantity(), d.getWeight(), rate));
        d.setStatus(DamageStatus.RECORDED);
        d.setRecordedAt(Instant.now());
        d.setRecordedBy(c.getPostedBy());
        d.setDamageNumber(numbering.next(DocumentType.STOCK_DAMAGE, d.getDamageDate()));

        d.setCreatedBy(c.getCreatedBy());
        d.setUpdatedBy(c.getUpdatedBy());

        StockDamage saved = damages.save(d);
        audit("STOCK_DAMAGE_RECORDED", "StockDamage", saved.getId(), "Created receiving-linked damage: " + saved.getDamageNumber(), http);
    }

    @Transactional
    public void reverseReceivingLinked(Long challanItemId, String reason, HttpServletRequest http) {
        damages.findAll((root, query, cb) -> cb.equal(root.get("sourceReceivingChallanItem").get("id"), challanItemId))
                .stream().findFirst().ifPresent(d -> {
                    if (d.getStatus() != DamageStatus.REVERSED) {
                        d.setStatus(DamageStatus.REVERSED);
                        d.setReversedAt(Instant.now());
                        d.setReversedBy(auditor());
                        d.setReversalReason(reason);
                        d.setUpdatedBy(auditor());
                        damages.save(d);
                        audit("STOCK_DAMAGE_REVERSED", "StockDamage", d.getId(), "Reversed receiving-linked damage: " + d.getDamageNumber() + ", Reason: " + reason, http);
                    }
                });
    }

    @Transactional(readOnly = true)
    public Page<StockDamageResponse> list(Specification<StockDamage> spec, Pageable pageable) {
        return damages.findAll(spec, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public StockDamageResponse getById(Long id) {
        return damages.findByIdWithDetails(id).map(this::response).orElseThrow();
    }

    private BigDecimal calculateAmount(ChargeMethod method, BigDecimal qty, BigDecimal weight, BigDecimal rate) {
        if (method == null || rate == null) return BigDecimal.ZERO;
        return switch (method) {
            case PER_PIECE -> qty.multiply(rate);
            case PER_WEIGHT -> weight.multiply(rate);
            case FIXED -> rate;
            default -> BigDecimal.ZERO;
        };
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
        tx.setSourceType("STOCK_DAMAGE");
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

    private StockDamageResponse response(StockDamage d) {
        return new StockDamageResponse(
                d.getId(),
                d.getDamageNumber(),
                d.getSourceType().name(),
                d.getSourceReceivingChallan() == null ? null : d.getSourceReceivingChallan().getId(),
                d.getSourceReceivingChallan() == null ? null : d.getSourceReceivingChallan().getReceivingChallanNumber(),
                d.getSourceReceivingChallanItem() == null ? null : d.getSourceReceivingChallanItem().getId(),
                d.getAgreement() == null ? null : d.getAgreement().getId(),
                d.getAgreement() == null ? null : d.getAgreement().getAgreementNumber(),
                d.getParty().getId(),
                d.getParty().getLegalName(),
                d.getSite().getId(),
                d.getSite().getSiteName(),
                d.getItem().getId(),
                d.getItem().getItemCode(),
                d.getItem().getItemName(),
                d.getItem().getUnit(),
                d.getDamageDate(),
                d.getQuantity(),
                d.getWeight(),
                d.isRepairable(),
                d.getDamageType().name(),
                d.getConditionNotes(),
                d.getChargeMethod().name(),
                d.getDamageRate(),
                d.getCalculatedDamageAmount(),
                d.getEstimatedRepairCost(),
                d.getActualRepairCost(),
                d.getAttachment() == null ? null : d.getAttachment().getId(),
                d.getStatus().name(),
                d.getRecordedAt(),
                d.getRecordedBy(),
                d.getRepairStartedAt(),
                d.getRepairStartedBy(),
                d.getRepairedAt(),
                d.getRepairedBy(),
                d.getScrappedAt(),
                d.getScrappedBy(),
                d.getReversedAt(),
                d.getReversedBy(),
                d.getReversalReason(),
                d.getCreatedAt(),
                d.getCreatedBy(),
                d.getUpdatedAt(),
                d.getUpdatedBy(),
                d.getVersion()
        );
    }
}
