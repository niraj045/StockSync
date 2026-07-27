package com.stocksync.exception.service;

import com.stocksync.agreement.entity.Agreement;
import com.stocksync.agreement.entity.AgreementItem;
import com.stocksync.agreement.entity.AgreementStatus;
import com.stocksync.agreement.repository.AgreementRepository;
import com.stocksync.audit.service.UserActivityLogService;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.common.numbering.DocumentNumberService;
import com.stocksync.common.numbering.DocumentType;
import com.stocksync.exception.dto.*;
import com.stocksync.exception.entity.*;
import com.stocksync.exception.repository.SiteTransferItemRepository;
import com.stocksync.exception.repository.SiteTransferRepository;
import com.stocksync.inventory.entity.Item;
import com.stocksync.inventory.entity.StockTransaction;
import com.stocksync.inventory.entity.SiteStockBalance;
import com.stocksync.inventory.repository.ItemRepository;
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
public class SiteTransferService {

    private final SiteTransferRepository transfers;
    private final SiteTransferItemRepository transferItems;
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

    public SiteTransferService(
            SiteTransferRepository transfers,
            SiteTransferItemRepository transferItems,
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
        this.transfers = transfers;
        this.transferItems = transferItems;
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
    public SiteTransferResponse create(SiteTransferRequest r, HttpServletRequest http) {
        if (r.sourceSiteId().equals(r.destinationSiteId())) {
            throw new BusinessRuleException("SAME_SOURCE_AND_DESTINATION_SITE", "Source and destination sites must be different");
        }

        Agreement srcAg = agreements.findById(r.sourceAgreementId()).orElseThrow();
        Agreement destAg = agreements.findById(r.destinationAgreementId()).orElseThrow();
        
        if (srcAg.getStatus() != AgreementStatus.ACTIVE || destAg.getStatus() != AgreementStatus.ACTIVE) {
            throw new BusinessRuleException("ACTIVE_AGREEMENT_REQUIRED", "Both source and destination agreements must be ACTIVE");
        }

        Party srcP = parties.findById(r.sourcePartyId()).orElseThrow();
        Site srcS = sites.findById(r.sourceSiteId()).orElseThrow();
        Party destP = parties.findById(r.destinationPartyId()).orElseThrow();
        Site destS = sites.findById(r.destinationSiteId()).orElseThrow();

        SiteTransfer t = new SiteTransfer();
        t.setSourceAgreement(srcAg);
        t.setDestinationAgreement(destAg);
        t.setSourceParty(srcP);
        t.setSourceSite(srcS);
        t.setDestinationParty(destP);
        t.setDestinationSite(destS);
        t.setTransferDate(r.transferDate());
        t.setVehicleNumber(r.notes() != null ? r.vehicleNumber() : r.vehicleNumber());
        t.setVehicleNumber(r.vehicleNumber());
        t.setDriverName(r.driverName());
        t.setDriverPhone(r.driverPhone());
        t.setTransporterId(r.transporterId());
        t.setNotes(r.notes());
        t.setStatus(TransferStatus.DRAFT);
        t.setTransferNumber(numbering.next(DocumentType.SITE_TRANSFER, t.getTransferDate()));

        t.setCreatedBy(auditor());
        t.setUpdatedBy(auditor());

        SiteTransfer saved = transfers.save(t);

        int seq = 1;
        for (var reqItem : r.items()) {
            Item item = items.findById(reqItem.itemId()).orElseThrow();
            AgreementItem srcAgItem = srcAg.getItems().stream()
                    .filter(ai -> ai.getId().equals(reqItem.sourceAgreementItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("DESTINATION_AGREEMENT_ITEM_NOT_FOUND", "Source agreement item not found"));
            AgreementItem destAgItem = destAg.getItems().stream()
                    .filter(ai -> ai.getId().equals(reqItem.destinationAgreementItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("DESTINATION_AGREEMENT_ITEM_NOT_FOUND", "Destination agreement item not found"));

            if (!srcAgItem.getItem().getId().equals(item.getId()) || !destAgItem.getItem().getId().equals(item.getId())) {
                throw new BusinessRuleException("DESTINATION_AGREEMENT_ITEM_NOT_FOUND", "Item exchange mismatch on agreements");
            }

            BigDecimal qty = Optional.ofNullable(reqItem.quantity()).orElse(BigDecimal.ZERO);
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("INVALID_QUANTITY", "Transfer quantity must be positive");
            }

            SiteTransferItem itemLine = new SiteTransferItem();
            itemLine.setTransfer(saved);
            itemLine.setSourceAgreementItem(srcAgItem);
            itemLine.setDestinationAgreementItem(destAgItem);
            itemLine.setItem(item);
            itemLine.setItemCodeSnapshot(item.getItemCode());
            itemLine.setItemNameSnapshot(item.getItemName());
            itemLine.setDescriptionSnapshot(item.getItemName()); // Description placeholder
            itemLine.setSizeSnapshot(item.getSize());
            itemLine.setUnitSnapshot(item.getUnit());
            itemLine.setWeightPerPieceSnapshot(Optional.ofNullable(item.getWeightPerPiece()).orElse(BigDecimal.ZERO));
            itemLine.setQuantity(qty);
            itemLine.setTotalWeight(qty.multiply(itemLine.getWeightPerPieceSnapshot()));
            itemLine.setSequence(seq++);

            saved.addItem(itemLine);
        }

        SiteTransfer finalSaved = transfers.save(saved);
        audit("SITE_TRANSFER_CREATED", "SiteTransfer", finalSaved.getId(), "Created site transfer: " + finalSaved.getTransferNumber(), http);
        return response(finalSaved);
    }

    @Transactional
    public SiteTransferResponse update(Long id, SiteTransferRequest r, HttpServletRequest http) {
        SiteTransfer t = transfers.findById(id).orElseThrow();
        if (t.getStatus() != TransferStatus.DRAFT) {
            throw new BusinessRuleException("INVALID_TRANSFER_STATUS", "Only DRAFT transfers can be updated");
        }

        if (r.sourceSiteId().equals(r.destinationSiteId())) {
            throw new BusinessRuleException("SAME_SOURCE_AND_DESTINATION_SITE", "Source and destination sites must be different");
        }

        Agreement srcAg = agreements.findById(r.sourceAgreementId()).orElseThrow();
        Agreement destAg = agreements.findById(r.destinationAgreementId()).orElseThrow();
        
        if (srcAg.getStatus() != AgreementStatus.ACTIVE || destAg.getStatus() != AgreementStatus.ACTIVE) {
            throw new BusinessRuleException("ACTIVE_AGREEMENT_REQUIRED", "Both source and destination agreements must be ACTIVE");
        }

        t.setSourceAgreement(srcAg);
        t.setDestinationAgreement(destAg);
        t.setSourceParty(parties.findById(r.sourcePartyId()).orElseThrow());
        t.setSourceSite(sites.findById(r.sourceSiteId()).orElseThrow());
        t.setDestinationParty(parties.findById(r.destinationPartyId()).orElseThrow());
        t.setDestinationSite(sites.findById(r.destinationSiteId()).orElseThrow());
        t.setTransferDate(r.transferDate());
        t.setVehicleNumber(r.vehicleNumber());
        t.setDriverName(r.driverName());
        t.setDriverPhone(r.driverPhone());
        t.setTransporterId(r.transporterId());
        t.setNotes(r.notes());
        t.setUpdatedBy(auditor());

        t.getItems().clear();
        transferItems.flush();

        int seq = 1;
        for (var reqItem : r.items()) {
            Item item = items.findById(reqItem.itemId()).orElseThrow();
            AgreementItem srcAgItem = srcAg.getItems().stream()
                    .filter(ai -> ai.getId().equals(reqItem.sourceAgreementItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("DESTINATION_AGREEMENT_ITEM_NOT_FOUND", "Source agreement item not found"));
            AgreementItem destAgItem = destAg.getItems().stream()
                    .filter(ai -> ai.getId().equals(reqItem.destinationAgreementItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("DESTINATION_AGREEMENT_ITEM_NOT_FOUND", "Destination agreement item not found"));

            BigDecimal qty = Optional.ofNullable(reqItem.quantity()).orElse(BigDecimal.ZERO);
            if (qty.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("INVALID_QUANTITY", "Transfer quantity must be positive");
            }

            SiteTransferItem itemLine = new SiteTransferItem();
            itemLine.setTransfer(t);
            itemLine.setSourceAgreementItem(srcAgItem);
            itemLine.setDestinationAgreementItem(destAgItem);
            itemLine.setItem(item);
            itemLine.setItemCodeSnapshot(item.getItemCode());
            itemLine.setItemNameSnapshot(item.getItemName());
            itemLine.setDescriptionSnapshot(item.getItemName());
            itemLine.setSizeSnapshot(item.getSize());
            itemLine.setUnitSnapshot(item.getUnit());
            itemLine.setWeightPerPieceSnapshot(Optional.ofNullable(item.getWeightPerPiece()).orElse(BigDecimal.ZERO));
            itemLine.setQuantity(qty);
            itemLine.setTotalWeight(qty.multiply(itemLine.getWeightPerPieceSnapshot()));
            itemLine.setSequence(seq++);

            t.addItem(itemLine);
        }

        SiteTransfer finalSaved = transfers.save(t);
        audit("SITE_TRANSFER_UPDATED", "SiteTransfer", finalSaved.getId(), "Updated site transfer: " + finalSaved.getTransferNumber(), http);
        return response(finalSaved);
    }

    @Transactional
    public SiteTransferResponse post(Long id, HttpServletRequest http) {
        SiteTransfer t = transfers.findByIdWithDetails(id).orElseThrow();
        if (t.getStatus() != TransferStatus.DRAFT) {
            return response(t); // Idempotency
        }

        Site srcSite = t.getSourceSite();
        Site destSite = t.getDestinationSite();

        for (var line : t.getItems()) {
            Item item = line.getItem();
            BigDecimal qty = line.getQuantity();

            // Lock source and destination balances
            SiteStockBalance srcBalance = siteBalances.findForUpdate(srcSite.getId(), item.getId()).orElseGet(() -> {
                jdbc.update("INSERT IGNORE INTO site_stock_balances(site_id, item_id) VALUES(?, ?)", srcSite.getId(), item.getId());
                return siteBalances.findForUpdate(srcSite.getId(), item.getId()).orElseThrow();
            });

            SiteStockBalance destBalance = siteBalances.findForUpdate(destSite.getId(), item.getId()).orElseGet(() -> {
                jdbc.update("INSERT IGNORE INTO site_stock_balances(site_id, item_id) VALUES(?, ?)", destSite.getId(), item.getId());
                return siteBalances.findForUpdate(destSite.getId(), item.getId()).orElseThrow();
            });

            if (qty.compareTo(srcBalance.getPendingQuantity()) > 0) {
                throw new BusinessRuleException("INSUFFICIENT_SITE_STOCK", "Transfer quantity " + qty + " exceeds source site pending stock of " + srcBalance.getPendingQuantity() + " for item " + item.getItemCode());
            }

            // Save snapshots
            line.setSourcePendingBefore(srcBalance.getPendingQuantity());
            line.setDestinationPendingBefore(destBalance.getPendingQuantity());

            // Move balances between source & dest
            srcBalance.setPendingQuantity(srcBalance.getPendingQuantity().subtract(qty));
            siteBalances.save(srcBalance);

            destBalance.setPendingQuantity(destBalance.getPendingQuantity().add(qty));
            siteBalances.save(destBalance);

            line.setSourcePendingAfter(srcBalance.getPendingQuantity());
            line.setDestinationPendingAfter(destBalance.getPendingQuantity());

            // Ledger entries: SITE_TRANSFER_OUT and SITE_TRANSFER_IN share same source transfer reference
            postLedger(item, "SITE_TRANSFER_OUT", t.getTransferDate(), qty, "OUT", "PENDING_SITE", t.getId(), srcSite, t.getSourceParty());
            postLedger(item, "SITE_TRANSFER_IN", t.getTransferDate(), qty, "IN", "PENDING_SITE", t.getId(), destSite, t.getDestinationParty());
        }

        t.setStatus(TransferStatus.POSTED);
        t.setPostedAt(Instant.now());
        t.setPostedBy(auditor());
        t.setUpdatedBy(auditor());

        SiteTransfer saved = transfers.save(t);
        audit("SITE_TRANSFER_POSTED", "SiteTransfer", saved.getId(), "Posted site transfer: " + saved.getTransferNumber(), http);
        return response(saved);
    }

    @Transactional
    public SiteTransferResponse cancel(Long id, String reason, HttpServletRequest http) {
        SiteTransfer t = transfers.findByIdWithDetails(id).orElseThrow();
        if (t.getStatus() == TransferStatus.CANCELLED) {
            return response(t); // Idempotency
        }
        if (t.getStatus() != TransferStatus.POSTED) {
            throw new BusinessRuleException("INVALID_TRANSFER_STATUS", "Only POSTED site transfers can be cancelled");
        }
        if (reason == null || reason.trim().isBlank()) {
            throw new BusinessRuleException("REVERSAL_REASON_REQUIRED", "Cancellation reason is required");
        }

        Site srcSite = t.getSourceSite();
        Site destSite = t.getDestinationSite();

        for (var line : t.getItems()) {
            Item item = line.getItem();
            BigDecimal qty = line.getQuantity();

            // Lock source and destination balances
            SiteStockBalance srcBalance = siteBalances.findForUpdate(srcSite.getId(), item.getId()).orElseThrow();
            SiteStockBalance destBalance = siteBalances.findForUpdate(destSite.getId(), item.getId()).orElseThrow();

            if (qty.compareTo(destBalance.getPendingQuantity()) > 0) {
                throw new BusinessRuleException("INSUFFICIENT_SITE_STOCK", "Compensating transfer quantity " + qty + " exceeds destination site pending stock of " + destBalance.getPendingQuantity() + " during cancellation");
            }

            // Restore source and destination balances
            srcBalance.setPendingQuantity(srcBalance.getPendingQuantity().add(qty));
            siteBalances.save(srcBalance);

            destBalance.setPendingQuantity(destBalance.getPendingQuantity().subtract(qty));
            siteBalances.save(destBalance);

            // Reversal ledger entries
            postLedger(item, "REVERSAL", t.getTransferDate(), qty, "IN", "PENDING_SITE", t.getId(), srcSite, t.getSourceParty());
            postLedger(item, "REVERSAL", t.getTransferDate(), qty, "OUT", "PENDING_SITE", t.getId(), destSite, t.getDestinationParty());
        }

        t.setStatus(TransferStatus.CANCELLED);
        t.setCancelledAt(Instant.now());
        t.setCancelledBy(auditor());
        t.setCancellationReason(reason);
        t.setUpdatedBy(auditor());

        SiteTransfer saved = transfers.save(t);
        audit("SITE_TRANSFER_CANCELLED", "SiteTransfer", saved.getId(), "Cancelled site transfer: " + saved.getTransferNumber() + ", Reason: " + reason, http);
        return response(saved);
    }

    @Transactional(readOnly = true)
    public Page<SiteTransferResponse> list(Specification<SiteTransfer> spec, Pageable pageable) {
        return transfers.findAll(spec, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public SiteTransferResponse getById(Long id) {
        return transfers.findByIdWithDetails(id).map(this::response).orElseThrow();
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
        tx.setSourceType("SITE_TRANSFER");
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

    private SiteTransferResponse response(SiteTransfer t) {
        List<SiteTransferItemResponse> list = t.getItems().stream().map(line -> new SiteTransferItemResponse(
                line.getId(),
                line.getSourceAgreementItem().getId(),
                line.getDestinationAgreementItem().getId(),
                line.getItem().getId(),
                line.getItemCodeSnapshot(),
                line.getItemNameSnapshot(),
                line.getDescriptionSnapshot(),
                line.getSizeSnapshot(),
                line.getUnitSnapshot(),
                line.getWeightPerPieceSnapshot(),
                line.getQuantity(),
                line.getTotalWeight(),
                line.getSourcePendingBefore(),
                line.getSourcePendingAfter(),
                line.getDestinationPendingBefore(),
                line.getDestinationPendingAfter(),
                line.getSequence()
        )).toList();

        return new SiteTransferResponse(
                t.getId(),
                t.getTransferNumber(),
                t.getSourceAgreement().getId(),
                t.getSourceAgreement().getAgreementNumber(),
                t.getDestinationAgreement().getId(),
                t.getDestinationAgreement().getAgreementNumber(),
                t.getSourceParty().getId(),
                t.getSourceParty().getLegalName(),
                t.getSourceSite().getId(),
                t.getSourceSite().getSiteName(),
                t.getDestinationParty().getId(),
                t.getDestinationParty().getLegalName(),
                t.getDestinationSite().getId(),
                t.getDestinationSite().getSiteName(),
                t.getTransferDate(),
                t.getStatus().name(),
                t.getVehicleNumber(),
                t.getDriverName(),
                t.getDriverPhone(),
                t.getTransporterId(),
                t.getNotes(),
                t.getPostedAt(),
                t.getPostedBy(),
                t.getCancelledAt(),
                t.getCancelledBy(),
                t.getCancellationReason(),
                list,
                t.getCreatedAt(),
                t.getCreatedBy(),
                t.getUpdatedAt(),
                t.getUpdatedBy(),
                t.getVersion()
        );
    }
}
