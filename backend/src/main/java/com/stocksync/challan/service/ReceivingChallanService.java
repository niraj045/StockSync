package com.stocksync.challan.service;

import com.stocksync.agreement.entity.Agreement;
import com.stocksync.agreement.repository.AgreementRepository;
import com.stocksync.audit.service.UserActivityLogService;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.challan.dto.*;
import com.stocksync.challan.entity.*;
import com.stocksync.challan.repository.IssuedChallanRepository;
import com.stocksync.challan.repository.ReceivingChallanItemRepository;
import com.stocksync.challan.repository.ReceivingChallanRepository;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.common.numbering.DocumentNumberService;
import com.stocksync.common.numbering.DocumentType;
import com.stocksync.inventory.entity.*;
import com.stocksync.inventory.repository.*;
import com.stocksync.party.entity.Party;
import com.stocksync.party.repository.PartyRepository;
import com.stocksync.site.entity.Site;
import com.stocksync.site.repository.SiteRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.*;

@Service
public class ReceivingChallanService {

    private final ReceivingChallanRepository challans;
    private final ReceivingChallanItemRepository challanItems;
    private final IssuedChallanRepository issuedChallans;
    private final StockBalanceRepository balances;
    private final SiteStockBalanceRepository siteBalances;
    private final StockTransactionRepository transactions;
    private final ItemRepository items;
    private final DocumentNumberService numbers;
    private final UserActivityLogService audit;
    private final UserRepository users;
    private final AgreementRepository agreements;
    private final PartyRepository parties;
    private final SiteRepository sites;
    private final JdbcTemplate jdbc;
    private final jakarta.persistence.EntityManager em;

    public ReceivingChallanService(
            ReceivingChallanRepository challans,
            ReceivingChallanItemRepository challanItems,
            IssuedChallanRepository issuedChallans,
            StockBalanceRepository balances,
            SiteStockBalanceRepository siteBalances,
            StockTransactionRepository transactions,
            ItemRepository items,
            DocumentNumberService numbers,
            UserActivityLogService audit,
            UserRepository users,
            AgreementRepository agreements,
            PartyRepository parties,
            SiteRepository sites,
            JdbcTemplate jdbc,
            jakarta.persistence.EntityManager em) {
        this.challans = challans;
        this.challanItems = challanItems;
        this.issuedChallans = issuedChallans;
        this.balances = balances;
        this.siteBalances = siteBalances;
        this.transactions = transactions;
        this.items = items;
        this.numbers = numbers;
        this.audit = audit;
        this.users = users;
        this.agreements = agreements;
        this.parties = parties;
        this.sites = sites;
        this.jdbc = jdbc;
        this.em = em;
    }

    @Transactional(readOnly = true)
    public Page<ReceivingChallanResponse> list(
            String search,
            String receivingChallanNumber,
            Long partyId,
            Long siteId,
            Long agreementId,
            Long linkedIssuedChallanId,
            String sourceType,
            String status,
            LocalDate receiveDateFrom,
            LocalDate receiveDateTo,
            Pageable pageable) {

        Specification<ReceivingChallan> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (search != null && !search.isBlank()) {
                String likePattern = "%" + search.trim().toLowerCase() + "%";
                var partyJoin = root.join("party", jakarta.persistence.criteria.JoinType.LEFT);
                var siteJoin = root.join("site", jakarta.persistence.criteria.JoinType.LEFT);
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("receivingChallanNumber")), likePattern),
                    cb.like(cb.lower(partyJoin.get("legalName")), likePattern),
                    cb.like(cb.lower(siteJoin.get("siteName")), likePattern)
                ));
            }
            if (receivingChallanNumber != null && !receivingChallanNumber.isBlank()) {
                predicates.add(cb.equal(root.get("receivingChallanNumber"), receivingChallanNumber.trim()));
            }
            if (partyId != null) {
                predicates.add(cb.equal(root.get("party").get("id"), partyId));
            }
            if (siteId != null) {
                predicates.add(cb.equal(root.get("site").get("id"), siteId));
            }
            if (agreementId != null) {
                predicates.add(cb.equal(root.get("agreement").get("id"), agreementId));
            }
            if (linkedIssuedChallanId != null) {
                predicates.add(cb.equal(root.get("linkedIssuedChallan").get("id"), linkedIssuedChallanId));
            }
            if (sourceType != null && !sourceType.isBlank()) {
                predicates.add(cb.equal(root.get("sourceType"), SourceType.valueOf(sourceType)));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), ReceivingStatus.valueOf(status)));
            }
            if (receiveDateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("receiveDate"), receiveDateFrom));
            }
            if (receiveDateTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("receiveDate"), receiveDateTo));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return challans.findAll(spec, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public ReceivingChallanResponse get(Long id) {
        return challans.findById(id)
                .map(this::response)
                .orElseThrow(() -> new BusinessRuleException("CHALLAN_NOT_FOUND", "Receiving challan not found"));
    }

    @Transactional
    public ReceivingChallanResponse create(ReceivingChallanRequest r, HttpServletRequest http) {
        String actor = auditor();
        String number = numbers.next(DocumentType.RECEIVING_CHALLAN, r.receiveDate());

        ReceivingChallan c = new ReceivingChallan();
        c.setReceivingChallanNumber(number);
        c.setReceiveDate(r.receiveDate());
        c.setVehicleNumber(blank(r.vehicleNumber()));
        c.setDriverName(blank(r.driverName()));
        c.setDriverPhone(blank(r.driverPhone()));
        c.setTransporterId(r.transporterId());
        c.setSourceType(SourceType.valueOf(r.sourceType()));
        c.setNotes(blank(r.notes()));
        c.setCreatedBy(actor);

        mapRelations(c, r);
        c.setStatus(hasExtraQuantity(r) ? ReceivingStatus.EXTRA_APPROVAL_REQUIRED : ReceivingStatus.DRAFT);

        mapItems(c, r.items());

        ReceivingChallan saved = challans.save(c);
        audit("RECEIVING_CHALLAN_CREATED", "ReceivingChallan", saved.getId(), saved.getReceivingChallanNumber(), http);
        return response(saved);
    }

    @Transactional
    public ReceivingChallanResponse update(Long id, ReceivingChallanRequest r, HttpServletRequest http) {
        ReceivingChallan c = challans.findById(id)
                .orElseThrow(() -> new BusinessRuleException("CHALLAN_NOT_FOUND", "Receiving challan not found"));

        if (c.getStatus() == ReceivingStatus.POSTED || c.getStatus() == ReceivingStatus.CANCELLED) {
            throw new BusinessRuleException("CHALLAN_IMMUTABLE", "Cannot edit posted or cancelled receiving challan");
        }

        c.setReceiveDate(r.receiveDate());
        c.setVehicleNumber(blank(r.vehicleNumber()));
        c.setDriverName(blank(r.driverName()));
        c.setDriverPhone(blank(r.driverPhone()));
        c.setTransporterId(r.transporterId());
        c.setSourceType(SourceType.valueOf(r.sourceType()));
        c.setNotes(blank(r.notes()));

        mapRelations(c, r);
        c.setStatus(hasExtraQuantity(r) ? ReceivingStatus.EXTRA_APPROVAL_REQUIRED : ReceivingStatus.DRAFT);

        c.getItems().clear();
        mapItems(c, r.items());

        ReceivingChallan saved = challans.save(c);
        audit("RECEIVING_CHALLAN_UPDATED", "ReceivingChallan", saved.getId(), saved.getReceivingChallanNumber(), http);
        return response(saved);
    }

    @Transactional
    public ReceivingChallanResponse approveExtra(Long id, HttpServletRequest http) {
        ReceivingChallan c = challans.findById(id)
                .orElseThrow(() -> new BusinessRuleException("CHALLAN_NOT_FOUND", "Receiving challan not found"));

        if (c.getStatus() != ReceivingStatus.EXTRA_APPROVAL_REQUIRED) {
            throw new BusinessRuleException("INVALID_STATUS", "Approval is only valid for EXTRA_APPROVAL_REQUIRED status");
        }

        c.setStatus(ReceivingStatus.APPROVED_FOR_POSTING);
        ReceivingChallan saved = challans.save(c);

        audit("RECEIVING_CHALLAN_EXTRA_APPROVED", "ReceivingChallan", saved.getId(), saved.getReceivingChallanNumber(), http);
        return response(saved);
    }

    @Transactional
    public ReceivingChallanResponse post(Long id, HttpServletRequest http) {
        ReceivingChallan c = challans.findById(id)
                .orElseThrow(() -> new BusinessRuleException("CHALLAN_NOT_FOUND", "Receiving challan not found"));

        if (c.getStatus() == ReceivingStatus.EXTRA_APPROVAL_REQUIRED) {
            throw new BusinessRuleException("EXTRA_APPROVAL_REQUIRED", "Admin approval is required for extra quantities");
        }
        if (c.getStatus() == ReceivingStatus.POSTED) {
            return response(c); // Idempotency: repeated calls returns already posted response
        }
        if (c.getStatus() == ReceivingStatus.CANCELLED) {
            throw new BusinessRuleException("CHALLAN_CANCELLED", "Cannot post a cancelled receiving challan");
        }

        String actor = auditor();

        for (var line : c.getItems()) {
            Item item = line.getItem();

            // Lock global stock balance
            StockBalance balance = balances.findForUpdate(item.getId()).orElseGet(() -> {
                jdbc.update("INSERT IGNORE INTO stock_balances(item_id) VALUES(?)", item.getId());
                return balances.findForUpdate(item.getId()).orElseThrow();
            });

            // Lock site stock balance
            SiteStockBalance siteBalance = siteBalances.findForUpdate(c.getSite().getId(), item.getId()).orElseGet(() -> {
                jdbc.update("INSERT IGNORE INTO site_stock_balances(site_id, item_id) VALUES(?, ?)", c.getSite().getId(), item.getId());
                return siteBalances.findForUpdate(c.getSite().getId(), item.getId()).orElseThrow();
            });

            BigDecimal normalQty = line.getGoodReturnedQuantity().add(line.getDamagedReturnedQuantity()).add(line.getLostQuantity());
            if (line.getExchangedFromItem() != null && line.getExchangedFromItem().getId().equals(item.getId())) {
                normalQty = normalQty.add(line.getExchangedQuantity());
            }

            if (normalQty.compareTo(siteBalance.getPendingQuantity()) > 0) {
                throw new BusinessRuleException("QUANTITY_EXCEEDED", "Normal return quantity " + normalQty + " exceeds site pending quantity " + siteBalance.getPendingQuantity() + " for item " + item.getItemCode());
            }

            // Concurrency-safe updates
            // 1. Reduce site pending
            siteBalance.setPendingQuantity(siteBalance.getPendingQuantity().subtract(normalQty));
            siteBalances.save(siteBalance);

            // 2. Update global stock balances
            balance.setAvailableQuantity(balance.getAvailableQuantity().add(line.getGoodReturnedQuantity()));
            balance.setIssuedQuantity(balance.getIssuedQuantity().subtract(normalQty));
            balance.setLostQuantity(balance.getLostQuantity().add(line.getLostQuantity()));

            if (line.getExtraReturnedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                // Extra good returns directly increase godown available stock
                balance.setAvailableQuantity(balance.getAvailableQuantity().add(line.getExtraReturnedQuantity()));
            }

            balance.setAvailableWeight(balance.getAvailableQuantity().multiply(Optional.ofNullable(item.getWeightPerPiece()).orElse(BigDecimal.ZERO)));

            // 3. Post stock ledger entries
            // Good Returns
            if (line.getGoodReturnedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                postLedger(item, "RECEIVE", c.getReceiveDate(), line.getGoodReturnedQuantity(), "IN", "AVAILABLE", c.getId(), c.getSite(), c.getParty(), actor);
                postLedger(item, "RECEIVE", c.getReceiveDate(), line.getGoodReturnedQuantity(), "OUT", "PENDING_SITE", c.getId(), c.getSite(), c.getParty(), actor);
            }
            // Damaged Returns
            if (line.getDamagedReturnedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                postLedger(item, "RECEIVE", c.getReceiveDate(), line.getDamagedReturnedQuantity(), "IN", "DAMAGED", c.getId(), c.getSite(), c.getParty(), actor);
                postLedger(item, "RECEIVE", c.getReceiveDate(), line.getDamagedReturnedQuantity(), "OUT", "PENDING_SITE", c.getId(), c.getSite(), c.getParty(), actor);
            }
            // Declared Losses
            if (line.getLostQuantity().compareTo(BigDecimal.ZERO) > 0) {
                postLedger(item, "RECEIVE", c.getReceiveDate(), line.getLostQuantity(), "IN", "LOST", c.getId(), c.getSite(), c.getParty(), actor);
                postLedger(item, "RECEIVE", c.getReceiveDate(), line.getLostQuantity(), "OUT", "PENDING_SITE", c.getId(), c.getSite(), c.getParty(), actor);
            }
            // Extra Returns
            if (line.getExtraReturnedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                postLedger(item, "RECEIVE", c.getReceiveDate(), line.getExtraReturnedQuantity(), "IN", "AVAILABLE", c.getId(), c.getSite(), c.getParty(), actor);
            }

            // 4. Exchanged line handling
            if (line.getExchangedFromItem() != null && line.getExchangedToItem() != null && line.getExchangedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                Item fromItem = line.getExchangedFromItem();
                Item toItem = line.getExchangedToItem();
                BigDecimal qty = line.getExchangedQuantity();

                // 4a. Returned exchangedFromItem: (Done above in normalQty updates)
                postLedger(fromItem, "RECEIVE", c.getReceiveDate(), qty, "IN", "AVAILABLE", c.getId(), c.getSite(), c.getParty(), actor);
                postLedger(fromItem, "RECEIVE", c.getReceiveDate(), qty, "OUT", "PENDING_SITE", c.getId(), c.getSite(), c.getParty(), actor);

                // 4b. Dispatched/issued exchangedToItem:
                StockBalance toBalance = balances.findForUpdate(toItem.getId()).orElseGet(() -> {
                    jdbc.update("INSERT IGNORE INTO stock_balances(item_id) VALUES(?)", toItem.getId());
                    return balances.findForUpdate(toItem.getId()).orElseThrow();
                });

                if (toBalance.getAvailableQuantity().compareTo(qty) < 0) {
                    throw new BusinessRuleException("INSUFFICIENT_STOCK", "Insufficient available stock for exchanged target item: " + toItem.getItemCode());
                }

                SiteStockBalance toSiteBalance = siteBalances.findForUpdate(c.getSite().getId(), toItem.getId()).orElseGet(() -> {
                    jdbc.update("INSERT IGNORE INTO site_stock_balances(site_id, item_id) VALUES(?, ?)", c.getSite().getId(), toItem.getId());
                    return siteBalances.findForUpdate(c.getSite().getId(), toItem.getId()).orElseThrow();
                });

                toBalance.setAvailableQuantity(toBalance.getAvailableQuantity().subtract(qty));
                toBalance.setIssuedQuantity(toBalance.getIssuedQuantity().add(qty));
                toBalance.setAvailableWeight(toBalance.getAvailableQuantity().multiply(Optional.ofNullable(toItem.getWeightPerPiece()).orElse(BigDecimal.ZERO)));

                toSiteBalance.setPendingQuantity(toSiteBalance.getPendingQuantity().add(qty));
                siteBalances.save(toSiteBalance);

                postLedger(toItem, "RECEIVE", c.getReceiveDate(), qty, "OUT", "AVAILABLE", c.getId(), c.getSite(), c.getParty(), actor);
                postLedger(toItem, "RECEIVE", c.getReceiveDate(), qty, "IN", "PENDING_SITE", c.getId(), c.getSite(), c.getParty(), actor);
            }
        }

        c.setStatus(ReceivingStatus.POSTED);
        c.setPostedAt(Instant.now());
        c.setPostedBy(actor);
        ReceivingChallan saved = challans.save(c);

        audit("RECEIVING_CHALLAN_POSTED", "ReceivingChallan", saved.getId(), saved.getReceivingChallanNumber(), http);
        return response(saved);
    }

    @Transactional
    public ReceivingChallanResponse cancel(Long id, String reason, HttpServletRequest http) {
        ReceivingChallan c = challans.findById(id)
                .orElseThrow(() -> new BusinessRuleException("CHALLAN_NOT_FOUND", "Receiving challan not found"));

        if (c.getStatus() != ReceivingStatus.POSTED) {
            throw new BusinessRuleException("INVALID_STATUS", "Only posted challans can be cancelled/reversed");
        }

        String actor = auditor();

        for (var line : c.getItems()) {
            Item item = line.getItem();

            // Lock balances
            StockBalance balance = balances.findForUpdate(item.getId()).orElseThrow();
            SiteStockBalance siteBalance = siteBalances.findForUpdate(c.getSite().getId(), item.getId()).orElseThrow();

            BigDecimal normalQty = line.getGoodReturnedQuantity().add(line.getDamagedReturnedQuantity()).add(line.getLostQuantity());
            if (line.getExchangedFromItem() != null && line.getExchangedFromItem().getId().equals(item.getId())) {
                normalQty = normalQty.add(line.getExchangedQuantity());
            }

            // Restore/revert quantities
            siteBalance.setPendingQuantity(siteBalance.getPendingQuantity().add(normalQty));
            siteBalances.save(siteBalance);

            balance.setAvailableQuantity(balance.getAvailableQuantity().subtract(line.getGoodReturnedQuantity()));
            balance.setIssuedQuantity(balance.getIssuedQuantity().add(normalQty));
            balance.setLostQuantity(balance.getLostQuantity().subtract(line.getLostQuantity()));

            if (line.getExtraReturnedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                balance.setAvailableQuantity(balance.getAvailableQuantity().subtract(line.getExtraReturnedQuantity()));
            }

            balance.setAvailableWeight(balance.getAvailableQuantity().multiply(Optional.ofNullable(item.getWeightPerPiece()).orElse(BigDecimal.ZERO)));

            // Write compensating ledger transactions (direction reversed)
            if (line.getGoodReturnedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                postLedger(item, "REVERSAL", c.getReceiveDate(), line.getGoodReturnedQuantity(), "OUT", "AVAILABLE", c.getId(), c.getSite(), c.getParty(), actor);
                postLedger(item, "REVERSAL", c.getReceiveDate(), line.getGoodReturnedQuantity(), "IN", "PENDING_SITE", c.getId(), c.getSite(), c.getParty(), actor);
            }
            if (line.getDamagedReturnedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                postLedger(item, "REVERSAL", c.getReceiveDate(), line.getDamagedReturnedQuantity(), "OUT", "DAMAGED", c.getId(), c.getSite(), c.getParty(), actor);
                postLedger(item, "REVERSAL", c.getReceiveDate(), line.getDamagedReturnedQuantity(), "IN", "PENDING_SITE", c.getId(), c.getSite(), c.getParty(), actor);
            }
            if (line.getLostQuantity().compareTo(BigDecimal.ZERO) > 0) {
                postLedger(item, "REVERSAL", c.getReceiveDate(), line.getLostQuantity(), "OUT", "LOST", c.getId(), c.getSite(), c.getParty(), actor);
                postLedger(item, "REVERSAL", c.getReceiveDate(), line.getLostQuantity(), "IN", "PENDING_SITE", c.getId(), c.getSite(), c.getParty(), actor);
            }
            if (line.getExtraReturnedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                postLedger(item, "REVERSAL", c.getReceiveDate(), line.getExtraReturnedQuantity(), "OUT", "AVAILABLE", c.getId(), c.getSite(), c.getParty(), actor);
            }

            // Reverse exchanged items
            if (line.getExchangedFromItem() != null && line.getExchangedToItem() != null && line.getExchangedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                Item fromItem = line.getExchangedFromItem();
                Item toItem = line.getExchangedToItem();
                BigDecimal qty = line.getExchangedQuantity();

                // Revert fromItem: (sitePending increase done in normalQty restore)
                postLedger(fromItem, "REVERSAL", c.getReceiveDate(), qty, "OUT", "AVAILABLE", c.getId(), c.getSite(), c.getParty(), actor);
                postLedger(fromItem, "REVERSAL", c.getReceiveDate(), qty, "IN", "PENDING_SITE", c.getId(), c.getSite(), c.getParty(), actor);

                // Revert toItem:
                StockBalance toBalance = balances.findForUpdate(toItem.getId()).orElseThrow();
                SiteStockBalance toSiteBalance = siteBalances.findForUpdate(c.getSite().getId(), toItem.getId()).orElseThrow();

                toBalance.setAvailableQuantity(toBalance.getAvailableQuantity().add(qty));
                toBalance.setIssuedQuantity(toBalance.getIssuedQuantity().subtract(qty));
                toBalance.setAvailableWeight(toBalance.getAvailableQuantity().multiply(Optional.ofNullable(toItem.getWeightPerPiece()).orElse(BigDecimal.ZERO)));

                toSiteBalance.setPendingQuantity(toSiteBalance.getPendingQuantity().subtract(qty));
                siteBalances.save(toSiteBalance);

                postLedger(toItem, "REVERSAL", c.getReceiveDate(), qty, "IN", "AVAILABLE", c.getId(), c.getSite(), c.getParty(), actor);
                postLedger(toItem, "REVERSAL", c.getReceiveDate(), qty, "OUT", "PENDING_SITE", c.getId(), c.getSite(), c.getParty(), actor);
            }
        }

        c.setStatus(ReceivingStatus.CANCELLED);
        c.setCancelledAt(Instant.now());
        c.setCancelledBy(actor);
        c.setCancellationReason(reason);
        ReceivingChallan saved = challans.save(c);

        audit("RECEIVING_CHALLAN_CANCELLED", "ReceivingChallan", saved.getId(), saved.getReceivingChallanNumber() + " - Reason: " + reason, http);
        return response(saved);
    }

    private void mapRelations(ReceivingChallan c, ReceivingChallanRequest r) {
        c.setParty(parties.findById(r.partyId()).orElseThrow(() -> new BusinessRuleException("PARTY_NOT_FOUND", "Party not found")));
        c.setSite(sites.findById(r.siteId()).orElseThrow(() -> new BusinessRuleException("SITE_NOT_FOUND", "Site not found")));

        if (r.agreementId() != null) {
            c.setAgreement(agreements.findById(r.agreementId()).orElseThrow(() -> new BusinessRuleException("AGREEMENT_NOT_FOUND", "Agreement not found")));
        } else {
            c.setAgreement(null);
        }

        if (r.linkedIssuedChallanId() != null) {
            c.setLinkedIssuedChallan(issuedChallans.findById(r.linkedIssuedChallanId()).orElseThrow(() -> new BusinessRuleException("CHALLAN_NOT_FOUND", "Linked issued challan not found")));
        } else {
            c.setLinkedIssuedChallan(null);
        }
    }

    private void mapItems(ReceivingChallan c, List<ReceivingChallanItemRequest> itemsList) {
        int seq = 1;
        for (var line : itemsList) {
            Item item = items.findById(line.itemId())
                    .orElseThrow(() -> new BusinessRuleException("ITEM_NOT_FOUND", "Item not found"));

            ReceivingChallanItem ci = new ReceivingChallanItem();
            ci.setItem(item);
            ci.setItemCodeSnapshot(item.getItemCode());
            ci.setItemNameSnapshot(item.getItemName());
            ci.setSizeSnapshot(item.getSize());
            ci.setUnitSnapshot(item.getUnit());
            ci.setWeightPerPieceSnapshot(item.getWeightPerPiece());

            // Get pending quantity snapshot
            BigDecimal pending = siteBalances.findBySiteIdAndItemId(c.getSite().getId(), item.getId())
                    .map(SiteStockBalance::getPendingQuantity)
                    .orElse(BigDecimal.ZERO);
            ci.setPendingQuantitySnapshot(pending);

            ci.setGoodReturnedQuantity(Optional.ofNullable(line.goodReturnedQuantity()).orElse(BigDecimal.ZERO));
            ci.setDamagedReturnedQuantity(Optional.ofNullable(line.damagedReturnedQuantity()).orElse(BigDecimal.ZERO));
            ci.setLostQuantity(Optional.ofNullable(line.lostQuantity()).orElse(BigDecimal.ZERO));
            ci.setExtraReturnedQuantity(Optional.ofNullable(line.extraReturnedQuantity()).orElse(BigDecimal.ZERO));

            // Weight snapshots
            BigDecimal weight = Optional.ofNullable(item.getWeightPerPiece()).orElse(BigDecimal.ZERO);
            ci.setGoodReturnedWeight(ci.getGoodReturnedQuantity().multiply(weight));
            ci.setDamagedWeight(ci.getDamagedReturnedQuantity().multiply(weight));
            ci.setLostWeight(ci.getLostQuantity().multiply(weight));

            ci.setOpeningImportTransactionId(line.openingImportTransactionId());
            ci.setNotes(blank(line.notes()));
            ci.setSequence(seq++);

            if (line.linkedIssuedChallanItemId() != null) {
                ci.setLinkedIssuedChallanItem(em.getReference(IssuedChallanItem.class, line.linkedIssuedChallanItemId()));
            }

            // Exchanged fields
            if (line.exchangedFromItemId() != null && line.exchangedToItemId() != null && line.exchangedQuantity() != null && line.exchangedQuantity().compareTo(BigDecimal.ZERO) > 0) {
                ci.setExchangedFromItem(items.findById(line.exchangedFromItemId()).orElseThrow());
                ci.setExchangedToItem(items.findById(line.exchangedToItemId()).orElseThrow());
                ci.setExchangedQuantity(line.exchangedQuantity());
            }

            c.addItem(ci);
        }
    }

    private boolean hasExtraQuantity(ReceivingChallanRequest r) {
        return r.items().stream()
                .anyMatch(line -> line.extraReturnedQuantity() != null && line.extraReturnedQuantity().compareTo(BigDecimal.ZERO) > 0);
    }

    private void postLedger(
            Item item,
            String txType,
            LocalDate txDate,
            BigDecimal qty,
            String direction,
            String bucket,
            Long sourceId,
            Site site,
            Party party,
            String actor) {

        BigDecimal weight = qty.multiply(Optional.ofNullable(item.getWeightPerPiece()).orElse(BigDecimal.ZERO));
        StockTransaction tx = new StockTransaction();
        tx.setItem(item);
        tx.setTransactionType(txType);
        tx.setTransactionDate(txDate);
        tx.setQuantity(qty);
        tx.setWeight(weight);
        tx.setDirection(direction);
        tx.setStockBucket(bucket);
        tx.setSourceType("RECEIVING_CHALLAN");
        tx.setSourceId(sourceId);
        tx.setSite(site);
        tx.setParty(party);
        tx.setCreatedBy(actor);
        transactions.save(tx);
    }

    private ReceivingChallanResponse response(ReceivingChallan c) {
        return new ReceivingChallanResponse(
                c.getId(),
                c.getReceivingChallanNumber(),
                c.getAgreement() == null ? null : c.getAgreement().getId(),
                c.getAgreement() == null ? null : c.getAgreement().getAgreementNumber(),
                c.getParty().getId(),
                c.getParty().getLegalName(),
                c.getSite().getId(),
                c.getSite().getSiteName(),
                c.getLinkedIssuedChallan() == null ? null : c.getLinkedIssuedChallan().getId(),
                c.getLinkedIssuedChallan() == null ? null : c.getLinkedIssuedChallan().getChallanNumber(),
                c.getReceiveDate(),
                c.getStatus().name(),
                c.getVehicleNumber(),
                c.getDriverName(),
                c.getDriverPhone(),
                c.getTransporterId(),
                c.getSourceType().name(),
                c.getNotes(),
                c.getPostedAt(),
                c.getPostedBy(),
                c.getCancelledAt(),
                c.getCancelledBy(),
                c.getCancellationReason(),
                c.getVersion(),
                c.getCreatedAt(),
                c.getCreatedBy(),
                c.getUpdatedAt(),
                c.getUpdatedBy(),
                c.getItems().stream().map(ci -> new ReceivingChallanItemResponse(
                        ci.getId(),
                        ci.getItem().getId(),
                        ci.getItemCodeSnapshot(),
                        ci.getItemNameSnapshot(),
                        ci.getSizeSnapshot(),
                        ci.getUnitSnapshot(),
                        ci.getLinkedIssuedChallanItem() == null ? null : ci.getLinkedIssuedChallanItem().getId(),
                        ci.getOpeningImportTransactionId(),
                        ci.getPendingQuantitySnapshot(),
                        ci.getGoodReturnedQuantity(),
                        ci.getDamagedReturnedQuantity(),
                        ci.getLostQuantity(),
                        ci.getExtraReturnedQuantity(),
                        ci.getExchangedFromItem() == null ? null : ci.getExchangedFromItem().getId(),
                        ci.getExchangedFromItem() == null ? null : ci.getExchangedFromItem().getItemCode(),
                        ci.getExchangedToItem() == null ? null : ci.getExchangedToItem().getId(),
                        ci.getExchangedToItem() == null ? null : ci.getExchangedToItem().getItemCode(),
                        ci.getExchangedQuantity(),
                        ci.getWeightPerPieceSnapshot(),
                        ci.getGoodReturnedWeight(),
                        ci.getDamagedWeight(),
                        ci.getLostWeight(),
                        ci.getNotes(),
                        ci.getSequence()
                )).toList()
        );
    }

    private void audit(String action, String entity, long id, String description, HttpServletRequest request) {
        User user = users.findByUsernameIgnoreCase(auditor()).orElse(null);
        audit.log(user == null ? null : user.getId(), auditor(), action, entity, String.valueOf(id), description, request);
    }

    private String auditor() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "system" : a.getName();
    }

    @Transactional(readOnly = true)
    public List<SiteStockBalanceResponse> getSitePendingBalances(Long siteId) {
        return siteBalances.findBySiteIdAndPendingQuantityGreaterThan(siteId, BigDecimal.ZERO).stream()
                .map(sb -> new SiteStockBalanceResponse(
                        sb.getId(),
                        sb.getItem().getId(),
                        sb.getItem().getItemCode(),
                        sb.getItem().getItemName(),
                        sb.getItem().getSize(),
                        sb.getItem().getUnit(),
                        sb.getPendingQuantity()
                )).toList();
    }

    @Transactional(readOnly = true)
    public List<IssuedChallanResponse> getIssuedChallansForSite(Long siteId) {
        return issuedChallans.findBySiteOrderSiteId(siteId).stream().map(c -> new IssuedChallanResponse(
                c.getId(),
                c.getChallanNumber(),
                c.getSiteOrder().getId(),
                c.getSiteOrder().getOrderNumber(),
                c.getSiteOrder().getSite().getId(),
                c.getSiteOrder().getSite().getSiteName(),
                c.getSiteOrder().getParty().getId(),
                c.getSiteOrder().getParty().getLegalName(),
                c.getDispatchDate(),
                c.getVehicleNumber(),
                c.getDriverName(),
                c.getNotes(),
                c.getCreatedBy(),
                c.getCreatedAt(),
                List.of()
        )).toList();
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
