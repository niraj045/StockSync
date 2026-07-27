package com.stocksync.challan.service;

import com.stocksync.audit.service.UserActivityLogService;
import com.stocksync.auth.entity.User;
import com.stocksync.auth.repository.UserRepository;
import com.stocksync.challan.dto.*;
import com.stocksync.challan.entity.*;
import com.stocksync.challan.repository.IssuedChallanRepository;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.common.numbering.DocumentNumberService;
import com.stocksync.common.numbering.DocumentType;
import com.stocksync.inventory.entity.*;
import com.stocksync.inventory.repository.*;
import com.stocksync.order.entity.*;
import com.stocksync.order.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
public class IssuedChallanService {
    private final IssuedChallanRepository challans;
    private final SiteOrderRepository orders;
    private final SiteOrderItemRepository orderItems;
    private final StockBalanceRepository balances;
    private final SiteStockBalanceRepository siteBalances;
    private final StockTransactionRepository transactions;
    private final ItemRepository items;
    private final DocumentNumberService numbers;
    private final UserActivityLogService audit;
    private final UserRepository users;
    private final JdbcTemplate jdbc;

    public IssuedChallanService(
            IssuedChallanRepository challans,
            SiteOrderRepository orders,
            SiteOrderItemRepository orderItems,
            StockBalanceRepository balances,
            SiteStockBalanceRepository siteBalances,
            StockTransactionRepository transactions,
            ItemRepository items,
            DocumentNumberService numbers,
            UserActivityLogService audit,
            UserRepository users,
            JdbcTemplate jdbc) {
        this.challans = challans;
        this.orders = orders;
        this.orderItems = orderItems;
        this.balances = balances;
        this.siteBalances = siteBalances;
        this.transactions = transactions;
        this.items = items;
        this.numbers = numbers;
        this.audit = audit;
        this.users = users;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public Page<IssuedChallanResponse> list(String search, Pageable pageable) {
        Page<IssuedChallan> page;
        if (search != null && !search.isBlank()) {
            page = challans.search("%" + search.trim() + "%", pageable);
        } else {
            page = challans.findAll(pageable);
        }
        return page.map(this::response);
    }

    @Transactional(readOnly = true)
    public IssuedChallanResponse get(Long id) {
        return challans.findById(id)
                .map(this::response)
                .orElseThrow(() -> new BusinessRuleException("CHALLAN_NOT_FOUND", "Issued challan not found"));
    }

    @Transactional
    public IssuedChallanResponse create(IssuedChallanRequest r, HttpServletRequest http) {
        SiteOrder order = orders.findById(r.siteOrderId())
                .orElseThrow(() -> new BusinessRuleException("ORDER_NOT_FOUND", "Site order not found"));

        if (order.getStatus() != OrderStatus.CONFIRMED && order.getStatus() != OrderStatus.PARTIALLY_FULFILLED) {
            throw new BusinessRuleException("INVALID_ORDER_STATUS", "Challan can only be issued against CONFIRMED or PARTIALLY_FULFILLED orders");
        }

        String actor = auditor();
        String number = numbers.next(DocumentType.ISSUED_CHALLAN, r.dispatchDate());

        IssuedChallan c = new IssuedChallan();
        c.setChallanNumber(number);
        c.setSiteOrder(order);
        c.setDispatchDate(r.dispatchDate());
        c.setVehicleNumber(blank(r.vehicleNumber()));
        c.setDriverName(blank(r.driverName()));
        c.setNotes(blank(r.notes()));
        c.setCreatedBy(actor);

        List<IssuedChallanItem> challanItems = new ArrayList<>();

        for (var line : r.items()) {
            Item item = items.findById(line.itemId())
                    .filter(Item::isActive)
                    .orElseThrow(() -> new BusinessRuleException("ITEM_NOT_FOUND_OR_INACTIVE", "Active item not found"));

            SiteOrderItem orderItem = order.getItems().stream()
                    .filter(oi -> oi.getItem().getId().equals(line.itemId()))
                    .findFirst()
                    .orElseThrow(() -> new BusinessRuleException("INVALID_ORDER_ITEM", "Item code " + item.getItemCode() + " is not in the site order"));

            BigDecimal remaining = orderItem.getOrderedQuantity().subtract(orderItem.getIssuedQuantity());
            if (line.quantity().compareTo(remaining) > 0) {
                throw new BusinessRuleException("DISPATCH_QUANTITY_EXCEEDED", "Requested quantity " + line.quantity() + " exceeds remaining order quantity " + remaining + " for item " + item.getItemCode());
            }

            // Lock global stock balance
            StockBalance balance = balances.findForUpdate(item.getId()).orElseGet(() -> {
                jdbc.update("INSERT IGNORE INTO stock_balances(item_id) VALUES(?)", item.getId());
                return balances.findForUpdate(item.getId()).orElseThrow();
            });

            if (balance.getAvailableQuantity().compareTo(line.quantity()) < 0) {
                throw new BusinessRuleException("INSUFFICIENT_STOCK", "Available stock for " + item.getItemCode() + " is " + balance.getAvailableQuantity() + ", requested " + line.quantity());
            }

            // Concurrency-safe updates
            // 1. Godown available stock reduction
            balance.setAvailableQuantity(balance.getAvailableQuantity().subtract(line.quantity()));
            BigDecimal weight = line.quantity().multiply(Optional.ofNullable(item.getWeightPerPiece()).orElse(BigDecimal.ZERO));
            balance.setAvailableWeight(balance.getAvailableQuantity().multiply(Optional.ofNullable(item.getWeightPerPiece()).orElse(BigDecimal.ZERO)));

            // 2. Global issued stock increase
            balance.setIssuedQuantity(balance.getIssuedQuantity().add(line.quantity()));
            balances.save(balance);

            // 3. Site-level pending stock increase
            SiteStockBalance siteBalance = siteBalances.findForUpdate(order.getSite().getId(), item.getId()).orElseGet(() -> {
                jdbc.update("INSERT IGNORE INTO site_stock_balances(site_id, item_id) VALUES(?, ?)", order.getSite().getId(), item.getId());
                return siteBalances.findForUpdate(order.getSite().getId(), item.getId()).orElseThrow();
            });
            siteBalance.setPendingQuantity(siteBalance.getPendingQuantity().add(line.quantity()));
            siteBalances.save(siteBalance);

            // 4. Update order item issued quantity
            orderItem.setIssuedQuantity(orderItem.getIssuedQuantity().add(line.quantity()));
            orderItems.save(orderItem);

            // 5. Post stock ledger transactions
            // Godown Available OUT
            StockTransaction txOut = new StockTransaction();
            txOut.setItem(item);
            txOut.setTransactionType("ISSUE");
            txOut.setTransactionDate(r.dispatchDate());
            txOut.setQuantity(line.quantity());
            txOut.setWeight(weight);
            txOut.setDirection("OUT");
            txOut.setStockBucket("AVAILABLE");
            txOut.setSourceType("ISSUED_CHALLAN");
            txOut.setSourceId(0L); // Will associate challan ID after save
            txOut.setCreatedBy(actor);
            transactions.save(txOut);

            // Site Pending IN
            StockTransaction txIn = new StockTransaction();
            txIn.setItem(item);
            txIn.setTransactionType("ISSUE");
            txIn.setTransactionDate(r.dispatchDate());
            txIn.setQuantity(line.quantity());
            txIn.setWeight(weight);
            txIn.setDirection("IN");
            txIn.setStockBucket("PENDING_SITE");
            txIn.setSourceType("ISSUED_CHALLAN");
            txIn.setSourceId(0L); // Will associate challan ID after save
            txIn.setSite(order.getSite());
            txIn.setParty(order.getParty());
            txIn.setCreatedBy(actor);
            transactions.save(txIn);

            // Create Challan Item
            IssuedChallanItem ci = new IssuedChallanItem();
            ci.setIssuedChallan(c);
            ci.setItem(item);
            ci.setQuantity(line.quantity());
            ci.setItemCodeSnapshot(item.getItemCode());
            ci.setItemNameSnapshot(item.getItemName());
            ci.setUnitSnapshot(item.getUnit());
            challanItems.add(ci);
        }

        c.setItems(challanItems);
        IssuedChallan saved = challans.save(c);

        // Update transaction sourceId
        jdbc.update("UPDATE stock_transactions SET source_id = ? WHERE source_type = 'ISSUED_CHALLAN' AND source_id = 0", saved.getId());

        // Update Site Order status
        boolean allFulfilled = order.getItems().stream()
                .allMatch(oi -> oi.getOrderedQuantity().compareTo(oi.getIssuedQuantity()) == 0);
        order.setStatus(allFulfilled ? OrderStatus.FULFILLED : OrderStatus.PARTIALLY_FULFILLED);
        orders.save(order);

        audit("STOCK_CHALLAN_ISSUED", "IssuedChallan", saved.getId(), saved.getChallanNumber(), http);
        return response(saved);
    }

    private IssuedChallanResponse response(IssuedChallan c) {
        return new IssuedChallanResponse(
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
                c.getItems().stream().map(ci -> new IssuedChallanItemResponse(
                        ci.getId(),
                        ci.getItem().getId(),
                        ci.getItemCodeSnapshot(),
                        ci.getItemNameSnapshot(),
                        ci.getUnitSnapshot(),
                        ci.getQuantity()
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

    private String blank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
