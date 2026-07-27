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
import com.stocksync.exception.dto.StockLossRequest;
import com.stocksync.exception.dto.StockLossResponse;
import com.stocksync.exception.entity.*;
import com.stocksync.exception.repository.StockLossRepository;
import com.stocksync.file.entity.FileAttachment;
import com.stocksync.file.repository.FileAttachmentRepository;
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
public class StockLossService {

    private final StockLossRepository losses;
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

    public StockLossService(
            StockLossRepository losses,
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
        this.losses = losses;
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
    public StockLossResponse create(StockLossRequest r, HttpServletRequest http) {
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

        BigDecimal rate = Optional.ofNullable(r.recoveryRate()).orElse(BigDecimal.ZERO);
        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("INVALID_RATE", "Recovery rate cannot be negative");
        }

        StockLoss l = new StockLoss();
        l.setSourceType(ExceptionSourceType.MANUAL_SITE_DECLARATION);
        l.setAgreement(ag);
        l.setParty(p);
        l.setSite(s);
        l.setItem(item);
        l.setLossDate(r.lossDate());
        l.setQuantity(qty);
        l.setWeight(wt);
        l.setChargeMethod(ChargeMethod.valueOf(r.chargeMethod()));
        l.setRecoveryRate(rate);
        l.setCalculatedRecoveryAmount(calculateAmount(l.getChargeMethod(), qty, wt, rate));
        l.setReason(r.reason());
        l.setAttachment(r.attachmentId() == null ? null : attachments.findById(r.attachmentId()).orElse(null));
        l.setStatus(LossStatus.DRAFT);
        l.setLossNumber(numbering.next(DocumentType.STOCK_LOSS, l.getLossDate()));

        l.setCreatedBy(auditor());
        l.setUpdatedBy(auditor());
        
        StockLoss saved = losses.save(l);
        audit("STOCK_LOSS_CREATED", "StockLoss", saved.getId(), "Created manual loss: " + saved.getLossNumber(), http);
        return response(saved);
    }

    @Transactional
    public StockLossResponse update(Long id, StockLossRequest r, HttpServletRequest http) {
        StockLoss l = losses.findById(id).orElseThrow();
        if (l.getStatus() != LossStatus.DRAFT) {
            throw new BusinessRuleException("INVALID_LOSS_STATUS", "Only DRAFT records can be updated");
        }

        BigDecimal qty = Optional.ofNullable(r.quantity()).orElse(BigDecimal.ZERO);
        BigDecimal wt = Optional.ofNullable(r.weight()).orElse(BigDecimal.ZERO);
        if (qty.compareTo(BigDecimal.ZERO) < 0 || wt.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("INVALID_QUANTITY", "Quantity and weight cannot be negative");
        }
        if (qty.compareTo(BigDecimal.ZERO) == 0 && wt.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessRuleException("INVALID_QUANTITY", "At least quantity or weight must be positive");
        }

        BigDecimal rate = Optional.ofNullable(r.recoveryRate()).orElse(BigDecimal.ZERO);
        if (rate.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("INVALID_RATE", "Recovery rate cannot be negative");
        }

        l.setLossDate(r.lossDate());
        l.setQuantity(qty);
        l.setWeight(wt);
        l.setChargeMethod(ChargeMethod.valueOf(r.chargeMethod()));
        l.setRecoveryRate(rate);
        l.setCalculatedRecoveryAmount(calculateAmount(l.getChargeMethod(), qty, wt, rate));
        l.setReason(r.reason());
        l.setAttachment(r.attachmentId() == null ? null : attachments.findById(r.attachmentId()).orElse(null));
        l.setUpdatedBy(auditor());

        StockLoss saved = losses.save(l);
        audit("STOCK_LOSS_UPDATED", "StockLoss", saved.getId(), "Updated manual loss: " + saved.getLossNumber(), http);
        return response(saved);
    }

    @Transactional
    public StockLossResponse approve(Long id, HttpServletRequest http) {
        StockLoss l = losses.findByIdWithDetails(id).orElseThrow();
        if (l.getStatus() != LossStatus.DRAFT) {
            return response(l); // Idempotency
        }

        Item item = l.getItem();
        Site site = l.getSite();
        BigDecimal qty = l.getQuantity();

        // Safe locking
        StockBalance balance = globalBalances.findForUpdate(item.getId()).orElseGet(() -> {
            jdbc.update("INSERT IGNORE INTO stock_balances(item_id) VALUES(?)", item.getId());
            return globalBalances.findForUpdate(item.getId()).orElseThrow();
        });

        SiteStockBalance siteBalance = siteBalances.findForUpdate(site.getId(), item.getId()).orElseGet(() -> {
            jdbc.update("INSERT IGNORE INTO site_stock_balances(site_id, item_id) VALUES(?, ?)", site.getId(), item.getId());
            return siteBalances.findForUpdate(site.getId(), item.getId()).orElseThrow();
        });

        if (qty.compareTo(siteBalance.getPendingQuantity()) > 0) {
            throw new BusinessRuleException("INSUFFICIENT_SITE_STOCK", "Loss quantity " + qty + " exceeds site pending stock of " + siteBalance.getPendingQuantity());
        }

        // Apply manual loss movements
        siteBalance.setPendingQuantity(siteBalance.getPendingQuantity().subtract(qty));
        siteBalances.save(siteBalance);

        balance.setLostQuantity(balance.getLostQuantity().add(qty));
        // Global AVAILABLE does not change.
        globalBalances.save(balance);

        // Ledger postings
        postLedger(item, "LOSS", l.getLossDate(), qty, "OUT", "PENDING_SITE", l.getId(), site, l.getParty());
        postLedger(item, "LOSS", l.getLossDate(), qty, "IN", "LOST", l.getId(), site, l.getParty());

        l.setStatus(LossStatus.APPROVED);
        l.setApprovedAt(Instant.now());
        l.setApprovedBy(auditor());
        l.setUpdatedBy(auditor());

        StockLoss saved = losses.save(l);
        audit("STOCK_LOSS_APPROVED", "StockLoss", saved.getId(), "Approved stock loss: " + saved.getLossNumber(), http);
        return response(saved);
    }

    @Transactional
    public StockLossResponse reverse(Long id, String reason, HttpServletRequest http) {
        StockLoss l = losses.findByIdWithDetails(id).orElseThrow();
        if (l.getStatus() == LossStatus.REVERSED) {
            return response(l); // Idempotency
        }
        if (l.getStatus() != LossStatus.APPROVED) {
            throw new BusinessRuleException("INVALID_LOSS_STATUS", "Only APPROVED records can be reversed");
        }
        if (l.getSourceType() == ExceptionSourceType.RECEIVING_CHALLAN) {
            throw new BusinessRuleException("INVALID_REVERSAL_REQUEST", "Linked receiving challan loss records cannot be reversed independently");
        }
        if (reason == null || reason.trim().isBlank()) {
            throw new BusinessRuleException("REVERSAL_REASON_REQUIRED", "Reversal reason is required");
        }

        Item item = l.getItem();
        Site site = l.getSite();
        BigDecimal qty = l.getQuantity();

        StockBalance balance = globalBalances.findForUpdate(item.getId()).orElseThrow();
        SiteStockBalance siteBalance = siteBalances.findForUpdate(site.getId(), item.getId()).orElseThrow();

        // Restore/revert quantities
        siteBalance.setPendingQuantity(siteBalance.getPendingQuantity().add(qty));
        siteBalances.save(siteBalance);

        balance.setLostQuantity(balance.getLostQuantity().subtract(qty));
        globalBalances.save(balance);

        // Compensating ledger entries (directions inverted)
        postLedger(item, "REVERSAL", l.getLossDate(), qty, "IN", "PENDING_SITE", l.getId(), site, l.getParty());
        postLedger(item, "REVERSAL", l.getLossDate(), qty, "OUT", "LOST", l.getId(), site, l.getParty());

        l.setStatus(LossStatus.REVERSED);
        l.setReversedAt(Instant.now());
        l.setReversedBy(auditor());
        l.setReversalReason(reason);
        l.setUpdatedBy(auditor());

        StockLoss saved = losses.save(l);
        audit("STOCK_LOSS_REVERSED", "StockLoss", saved.getId(), "Reversed stock loss: " + saved.getLossNumber() + ", Reason: " + reason, http);
        return response(saved);
    }

    @Transactional
    public void createReceivingLinked(ReceivingChallan c, ReceivingChallanItem item, HttpServletRequest http) {
        if (losses.existsBySourceReceivingChallanItemId(item.getId())) {
            return; // Prevent duplicates
        }

        StockLoss l = new StockLoss();
        l.setSourceType(ExceptionSourceType.RECEIVING_CHALLAN);
        l.setSourceReceivingChallan(c);
        l.setSourceReceivingChallanItem(item);
        l.setAgreement(c.getAgreement());
        l.setParty(c.getParty());
        l.setSite(c.getSite());
        l.setItem(item.getItem());
        l.setLossDate(c.getReceiveDate());
        l.setQuantity(item.getLostQuantity());
        l.setWeight(item.getLostWeight());
        
        // Use agreement snapshots or lookup agreement item details
        BigDecimal rate = BigDecimal.ZERO;
        ChargeMethod method = ChargeMethod.NONE;
        if (c.getAgreement() != null) {
            rate = c.getAgreement().getItems().stream()
                    .filter(ai -> ai.getItem().getId().equals(item.getItem().getId()))
                    .findFirst()
                    .map(ai -> {
                        if (ai.getLossRatePerPiece().compareTo(BigDecimal.ZERO) > 0) {
                            return ai.getLossRatePerPiece();
                        }
                        return ai.getLossRatePerWeight();
                    }).orElse(BigDecimal.ZERO);
            
            boolean perWeight = c.getAgreement().getItems().stream()
                    .filter(ai -> ai.getItem().getId().equals(item.getItem().getId()))
                    .findFirst()
                    .map(ai -> ai.getLossRatePerWeight().compareTo(BigDecimal.ZERO) > 0)
                    .orElse(false);
            if (rate.compareTo(BigDecimal.ZERO) > 0) {
                method = perWeight ? ChargeMethod.PER_WEIGHT : ChargeMethod.PER_PIECE;
            }
        }
        l.setChargeMethod(method);
        l.setRecoveryRate(rate);
        l.setCalculatedRecoveryAmount(calculateAmount(method, l.getQuantity(), l.getWeight(), rate));
        l.setReason(item.getNotes());
        l.setStatus(LossStatus.APPROVED);
        l.setApprovedAt(Instant.now());
        l.setApprovedBy(c.getPostedBy());
        l.setLossNumber(numbering.next(DocumentType.STOCK_LOSS, l.getLossDate()));

        l.setCreatedBy(c.getCreatedBy());
        l.setUpdatedBy(c.getUpdatedBy());

        StockLoss saved = losses.save(l);
        audit("STOCK_LOSS_APPROVED", "StockLoss", saved.getId(), "Created receiving-linked loss: " + saved.getLossNumber(), http);
    }

    @Transactional
    public void reverseReceivingLinked(Long challanItemId, String reason, HttpServletRequest http) {
        losses.findAll((root, query, cb) -> cb.equal(root.get("sourceReceivingChallanItem").get("id"), challanItemId))
                .stream().findFirst().ifPresent(l -> {
                    if (l.getStatus() != LossStatus.REVERSED) {
                        l.setStatus(LossStatus.REVERSED);
                        l.setReversedAt(Instant.now());
                        l.setReversedBy(auditor());
                        l.setReversalReason(reason);
                        l.setUpdatedBy(auditor());
                        losses.save(l);
                        audit("STOCK_LOSS_REVERSED", "StockLoss", l.getId(), "Reversed receiving-linked loss: " + l.getLossNumber() + ", Reason: " + reason, http);
                    }
                });
    }

    @Transactional(readOnly = true)
    public Page<StockLossResponse> list(Specification<StockLoss> spec, Pageable pageable) {
        return losses.findAll(spec, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public StockLossResponse getById(Long id) {
        return losses.findByIdWithDetails(id).map(this::response).orElseThrow();
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
        tx.setSourceType("STOCK_LOSS");
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

    private StockLossResponse response(StockLoss l) {
        return new StockLossResponse(
                l.getId(),
                l.getLossNumber(),
                l.getSourceType().name(),
                l.getSourceReceivingChallan() == null ? null : l.getSourceReceivingChallan().getId(),
                l.getSourceReceivingChallan() == null ? null : l.getSourceReceivingChallan().getReceivingChallanNumber(),
                l.getSourceReceivingChallanItem() == null ? null : l.getSourceReceivingChallanItem().getId(),
                l.getAgreement() == null ? null : l.getAgreement().getId(),
                l.getAgreement() == null ? null : l.getAgreement().getAgreementNumber(),
                l.getParty().getId(),
                l.getParty().getLegalName(),
                l.getSite().getId(),
                l.getSite().getSiteName(),
                l.getItem().getId(),
                l.getItem().getItemCode(),
                l.getItem().getItemName(),
                l.getItem().getUnit(),
                l.getLossDate(),
                l.getQuantity(),
                l.getWeight(),
                l.getChargeMethod().name(),
                l.getRecoveryRate(),
                l.getCalculatedRecoveryAmount(),
                l.getReason(),
                l.getAttachment() == null ? null : l.getAttachment().getId(),
                l.getStatus().name(),
                l.getApprovedAt(),
                l.getApprovedBy(),
                l.getReversedAt(),
                l.getReversedBy(),
                l.getReversalReason(),
                l.getCreatedAt(),
                l.getCreatedBy(),
                l.getUpdatedAt(),
                l.getUpdatedBy(),
                l.getVersion()
        );
    }
}
