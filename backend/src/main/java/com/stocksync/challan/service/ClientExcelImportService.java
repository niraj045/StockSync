package com.stocksync.challan.service;

import com.stocksync.challan.entity.*;
import com.stocksync.challan.repository.IssuedChallanRepository;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.inventory.entity.*;
import com.stocksync.inventory.repository.*;
import com.stocksync.order.entity.SiteOrder;
import com.stocksync.order.entity.OrderStatus;
import com.stocksync.order.repository.SiteOrderRepository;
import com.stocksync.party.entity.Party;
import com.stocksync.party.repository.PartyRepository;
import com.stocksync.site.entity.Site;
import com.stocksync.site.entity.SiteStatus;
import com.stocksync.site.repository.SiteRepository;
import com.stocksync.agreement.entity.Agreement;
import com.stocksync.agreement.entity.AgreementStatus;
import com.stocksync.quotation.entity.RentalType;
import com.stocksync.agreement.entity.BillingCycle;
import com.stocksync.agreement.entity.MeasurementBasis;
import com.stocksync.agreement.entity.BillingCommencementRule;
import com.stocksync.agreement.repository.AgreementRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

@Service
public class ClientExcelImportService {
    private final IssuedChallanRepository challans;
    private final SiteOrderRepository orders;
    private final ItemRepository items;
    private final StockBalanceRepository balances;
    private final SiteStockBalanceRepository siteBalances;
    private final StockTransactionRepository transactions;
    private final JdbcTemplate jdbc;
    private final PartyRepository parties;
    private final SiteRepository sites;
    private final AgreementRepository agreements;

    public ClientExcelImportService(
            IssuedChallanRepository challans,
            SiteOrderRepository orders,
            ItemRepository items,
            StockBalanceRepository balances,
            SiteStockBalanceRepository siteBalances,
            StockTransactionRepository transactions,
            JdbcTemplate jdbc,
            PartyRepository parties,
            SiteRepository sites,
            AgreementRepository agreements) {
        this.challans = challans;
        this.orders = orders;
        this.items = items;
        this.balances = balances;
        this.siteBalances = siteBalances;
        this.transactions = transactions;
        this.jdbc = jdbc;
        this.parties = parties;
        this.sites = sites;
        this.agreements = agreements;
    }

    @Transactional
    public int importClientExcel(Long siteOrderId, MultipartFile file) {
        List<Item> allItems = items.findAll();
        int importedCount = 0;
        String actor = auditor();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Find Header Row
            int headerRowIndex = -1;
            int challanNoCol = -1;
            int dateCol = -1;
            int particularsCol = -1;
            int qtyCol = -1;
            
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.STRING) {
                        String val = cell.getStringCellValue().trim().toLowerCase();
                        if (val.contains("challan no") || val.contains("challan number") || val.contains("challan") || val.contains("sr. no") || val.contains("sr.no")) {
                            headerRowIndex = row.getRowNum();
                            if (val.contains("challan")) challanNoCol = cell.getColumnIndex();
                            else if (challanNoCol == -1) challanNoCol = cell.getColumnIndex();
                        } else if (val.contains("date") && headerRowIndex == row.getRowNum()) {
                            dateCol = cell.getColumnIndex();
                        } else if ((val.contains("particular") || val.contains("item") || val.contains("description")) && headerRowIndex == row.getRowNum()) {
                            particularsCol = cell.getColumnIndex();
                        } else if ((val.contains("qty") || val.contains("quantity")) && headerRowIndex == row.getRowNum()) {
                            qtyCol = cell.getColumnIndex();
                        }
                    }
                }
                if (headerRowIndex != -1 && (challanNoCol != -1 || particularsCol != -1)) {
                    if (dateCol == -1) dateCol = challanNoCol;
                    break;
                }
            }
            
            if (headerRowIndex == -1) {
                // Fallback scan: search any row for 'particulars' or 'qty' or 'challan'
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        if (cell.getCellType() == CellType.STRING) {
                            String val = cell.getStringCellValue().trim().toLowerCase();
                            if (val.contains("particular") || val.contains("qty") || val.contains("challan")) {
                                headerRowIndex = row.getRowNum();
                                for (Cell c : row) {
                                    if (c.getCellType() == CellType.STRING) {
                                        String v = c.getStringCellValue().trim().toLowerCase();
                                        if (v.contains("challan")) challanNoCol = c.getColumnIndex();
                                        else if (v.contains("date")) dateCol = c.getColumnIndex();
                                        else if (v.contains("particular") || v.contains("item") || v.contains("description")) particularsCol = c.getColumnIndex();
                                        else if (v.contains("qty") || v.contains("quantity")) qtyCol = c.getColumnIndex();
                                    }
                                }
                                if (dateCol == -1) dateCol = challanNoCol != -1 ? challanNoCol : 0;
                                if (challanNoCol == -1) challanNoCol = dateCol != -1 ? dateCol : 0;
                                break;
                            }
                        }
                    }
                    if (headerRowIndex != -1) break;
                }
            }

            if (headerRowIndex == -1) {
                throw new BusinessRuleException("INVALID_FORMAT", "Could not find 'Challan no.' or 'Particulars' header in the first sheet.");
            }
            
            SiteOrder order = null;
            if (siteOrderId != null) {
                order = orders.findById(siteOrderId).orElse(null);
            }
            
            if (order == null) {
                // Auto-detect Target Site Order
                List<SiteOrder> activeOrders = orders.findAll().stream()
                        .filter(o -> !o.getStatus().name().equals("CANCELLED"))
                        .toList();
                
                for (int i = 0; i <= headerRowIndex; i++) {
                    Row row = sheet.getRow(i);
                    if (row == null) continue;
                    for (Cell cell : row) {
                        if (cell.getCellType() == CellType.STRING) {
                            String val = cell.getStringCellValue().trim().toLowerCase();
                            if (val.isEmpty()) continue;
                            
                            for (SiteOrder so : activeOrders) {
                                if (so.getSite() != null && so.getSite().getSiteName() != null && 
                                    val.contains(so.getSite().getSiteName().toLowerCase())) {
                                    order = so;
                                    break;
                                }
                            }
                        }
                        if (order != null) break;
                    }
                    if (order != null) break;
                }
            }
            
            if (order == null) {
                // Auto-create missing entities
                String rawName = "Unknown Client";
                if (headerRowIndex > 0) {
                    for (int i = 0; i < headerRowIndex; i++) {
                        Row r = sheet.getRow(i);
                        if (r == null) continue;
                        for (Cell c : r) {
                            if (c.getCellType() == CellType.STRING) {
                                String v = c.getStringCellValue().trim();
                                if (!v.isEmpty()) {
                                    rawName = v;
                                    break;
                                }
                            }
                        }
                        if (!rawName.equals("Unknown Client")) break;
                    }
                }
                
                final String finalRawName = rawName.length() > 100 ? rawName.substring(0, 100) : rawName;
                
                Party party = parties.findAll().stream()
                        .filter(p -> p.getLegalName().equalsIgnoreCase(finalRawName))
                        .findFirst()
                        .orElseGet(() -> {
                            Party p = new Party();
                            p.setLegalName(finalRawName);
                            p.setActive(true);
                            return parties.save(p);
                        });
                
                Site site = sites.findAll().stream()
                        .filter(s -> s.getParty().getId().equals(party.getId()) && s.getSiteName().equalsIgnoreCase(finalRawName))
                        .findFirst()
                        .orElseGet(() -> {
                            Site s = new Site();
                            s.setParty(party);
                            s.setSiteName(finalRawName);
                            s.setSiteCode("HIST-" + System.currentTimeMillis());
                            s.setStatus(SiteStatus.ACTIVE);
                            s.setDefaulter(false);
                            return sites.save(s);
                        });
                
                Agreement agreement = new Agreement();
                agreement.setAgreementNumber("AGR-HIST-" + System.currentTimeMillis());
                agreement.setParty(party);
                agreement.setSite(site);
                agreement.setAgreementDate(LocalDate.now());
                agreement.setEffectiveDate(LocalDate.now());
                agreement.setRentalType(RentalType.PER_PIECE_PER_MONTH);
                agreement.setBillingCycle(BillingCycle.MONTHLY);
                agreement.setMeasurementBasis(MeasurementBasis.ITEM_QUANTITY);
                agreement.setBillingCommencementRule(BillingCommencementRule.FIRST_DISPATCH);
                agreement.setStatus(AgreementStatus.DRAFT);
                agreement.setPartyLegalNameSnapshot(party.getLegalName());
                agreement.setSiteNameSnapshot(site.getSiteName());
                agreement.setSiteCodeSnapshot(site.getSiteCode());
                
                agreement = agreements.save(agreement);
                
                SiteOrder newOrder = new SiteOrder();
                newOrder.setOrderNumber("ORD-HIST-" + System.currentTimeMillis());
                newOrder.setParty(party);
                newOrder.setSite(site);
                newOrder.setAgreement(agreement);
                newOrder.setOrderDate(LocalDate.now());
                newOrder.setStatus(OrderStatus.DRAFT);
                newOrder.setNotes("Auto-created from Historical Excel Import (Please edit to add actual items and confirm)");
                
                order = orders.save(newOrder);
            }
            
            final SiteOrder finalOrder = order;
            Row headerRow = sheet.getRow(headerRowIndex);
            
            // Map columns to Items
            Map<Integer, Item> columnItemMap = new HashMap<>();
            for (Cell cell : headerRow) {
                if (cell.getColumnIndex() <= dateCol) continue;
                if (cell.getCellType() != CellType.STRING) continue;
                
                String headerName = cell.getStringCellValue().trim();
                if (headerName.isEmpty()) continue;
                
                // Normalize header for fuzzy matching
                String normHeader = headerName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                if (normHeader.endsWith("s")) normHeader = normHeader.substring(0, normHeader.length() - 1);
                
                // Fuzzy match item
                Item matched = null;
                for (Item item : allItems) {
                    if (item.getItemName().equalsIgnoreCase(headerName) || item.getItemCode().equalsIgnoreCase(headerName)) {
                        matched = item;
                        break;
                    }
                }
                // Fallback: partial match
                if (matched == null) {
                    for (Item item : allItems) {
                        String normItem = item.getItemName().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                        if (normItem.endsWith("s")) normItem = normItem.substring(0, normItem.length() - 1);
                        
                        if (normItem.contains(normHeader) || normHeader.contains(normItem)) {
                            matched = item;
                            break;
                        }
                    }
                }
                
                if (matched != null) {
                    columnItemMap.put(cell.getColumnIndex(), matched);
                }
            }

            // Process Data Rows
            for (int r = headerRowIndex + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                
                Cell challanCell = row.getCell(challanNoCol);
                if (challanCell == null) continue;
                
                String challanNo = getAsString(challanCell);
                if (challanNo.isEmpty() || challanNo.toLowerCase().contains("total")) {
                    // Reached the end (e.g., "Total Material Delivered")
                    break;
                }
                
                Cell dateCell = row.getCell(dateCol);
                LocalDate dispatchDate = null;
                if (dateCell != null) {
                    if (dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                        dispatchDate = dateCell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    } else {
                        try {
                            dispatchDate = LocalDate.parse(getAsString(dateCell));
                        } catch (Exception e) {
                            dispatchDate = LocalDate.now(); // Fallback
                        }
                    }
                } else {
                    dispatchDate = LocalDate.now();
                }
                
                IssuedChallan c = new IssuedChallan();
                c.setChallanNumber(challanNo);
                c.setSiteOrder(finalOrder);
                c.setDispatchDate(dispatchDate);
                c.setNotes("Historical Import");
                c.setCreatedBy(actor);
                
                List<IssuedChallanItem> challanItems = new ArrayList<>();
                
                for (Map.Entry<Integer, Item> entry : columnItemMap.entrySet()) {
                    Cell qtyCell = row.getCell(entry.getKey());
                    if (qtyCell == null) continue;
                    
                    BigDecimal qty = BigDecimal.ZERO;
                    if (qtyCell.getCellType() == CellType.NUMERIC) {
                        qty = BigDecimal.valueOf(qtyCell.getNumericCellValue());
                    } else if (qtyCell.getCellType() == CellType.STRING) {
                        try {
                            qty = new BigDecimal(qtyCell.getStringCellValue().trim());
                        } catch (Exception ignored) {}
                    }
                    
                    if (qty.compareTo(BigDecimal.ZERO) > 0) {
                        Item item = entry.getValue();
                        
                        boolean hasOpeningStock = Boolean.TRUE.equals(jdbc.queryForObject(
                            "SELECT COUNT(*) > 0 FROM stock_transactions WHERE site_id = ? AND item_id = ? AND transaction_type LIKE 'OPENING%'",
                            Boolean.class, finalOrder.getSite().getId(), item.getId()
                        ));

                        if (!hasOpeningStock) {
                            // Update Balances only if opening stock has not set baseline snapshot
                            StockBalance balance = balances.findForUpdate(item.getId()).orElseGet(() -> {
                                jdbc.update("INSERT IGNORE INTO stock_balances(item_id) VALUES(?)", item.getId());
                                return balances.findForUpdate(item.getId()).orElseThrow();
                            });
                            balance.setAvailableQuantity(balance.getAvailableQuantity().subtract(qty));
                            balance.setIssuedQuantity(balance.getIssuedQuantity().add(qty));
                            balances.save(balance);

                            SiteStockBalance siteBalance = siteBalances.findForUpdate(finalOrder.getSite().getId(), item.getId()).orElseGet(() -> {
                                jdbc.update("INSERT IGNORE INTO site_stock_balances(site_id, item_id) VALUES(?, ?)", finalOrder.getSite().getId(), item.getId());
                                return siteBalances.findForUpdate(finalOrder.getSite().getId(), item.getId()).orElseThrow();
                            });
                            siteBalance.setPendingQuantity(siteBalance.getPendingQuantity().add(qty));
                            siteBalances.save(siteBalance);
                        }
                        
                        BigDecimal weight = qty.multiply(Optional.ofNullable(item.getWeightPerPiece()).orElse(BigDecimal.ZERO));
                        
                        StockTransaction txOut = new StockTransaction();
                        txOut.setItem(item);
                        txOut.setTransactionType("ISSUE");
                        txOut.setTransactionDate(dispatchDate);
                        txOut.setQuantity(qty);
                        txOut.setWeight(weight);
                        txOut.setDirection("OUT");
                        txOut.setStockBucket("AVAILABLE");
                        txOut.setSourceType("ISSUED_CHALLAN");
                        txOut.setSourceId(0L); // Associate later
                        txOut.setCreatedBy(actor);
                        transactions.save(txOut);

                        StockTransaction txIn = new StockTransaction();
                        txIn.setItem(item);
                        txIn.setTransactionType("ISSUE");
                        txIn.setTransactionDate(dispatchDate);
                        txIn.setQuantity(qty);
                        txIn.setWeight(weight);
                        txIn.setDirection("IN");
                        txIn.setStockBucket("PENDING_SITE");
                        txIn.setSourceType("ISSUED_CHALLAN");
                        txIn.setSourceId(0L); 
                        txIn.setSite(finalOrder.getSite());
                        txIn.setParty(finalOrder.getParty());
                        txIn.setCreatedBy(actor);
                        transactions.save(txIn);
                        
                        IssuedChallanItem ci = new IssuedChallanItem();
                        ci.setIssuedChallan(c);
                        ci.setItem(item);
                        ci.setQuantity(qty);
                        ci.setItemCodeSnapshot(item.getItemCode());
                        ci.setItemNameSnapshot(item.getItemName());
                        ci.setUnitSnapshot(item.getUnit());
                        challanItems.add(ci);
                    }
                }
                
                if (!challanItems.isEmpty()) {
                    c.setItems(challanItems);
                    IssuedChallan saved = challans.save(c);
                    jdbc.update("UPDATE stock_transactions SET source_id = ? WHERE source_type = 'ISSUED_CHALLAN' AND source_id = 0", saved.getId());
                    importedCount++;
                }
            }

            // Sync imported items into SiteOrderItem and set order status to CONFIRMED
            if (finalOrder != null) {
                jdbc.execute("UPDATE site_orders SET status = 'CONFIRMED' WHERE id = " + finalOrder.getId());
                // Insert items into site_order_items if missing or update ordered_quantity
                for (Item item : columnItemMap.values()) {
                    jdbc.update("""
                        INSERT INTO site_order_items (order_id, item_id, ordered_quantity, issued_quantity, version)
                        SELECT ?, ?, COALESCE(SUM(ici.quantity), 0), COALESCE(SUM(ici.quantity), 0), 0
                        FROM issued_challans ic
                        JOIN issued_challan_items ici ON ici.issued_challan_id = ic.id
                        WHERE ic.site_order_id = ? AND ici.item_id = ?
                        ON DUPLICATE KEY UPDATE ordered_quantity = VALUES(ordered_quantity), issued_quantity = VALUES(issued_quantity)
                    """, finalOrder.getId(), item.getId(), finalOrder.getId(), item.getId());
                }
            }
            
        } catch (Exception e) {
            throw new BusinessRuleException("IMPORT_FAILED", "Failed to parse Excel file: " + e.getMessage());
        }
        
        return importedCount;
    }

    private String getAsString(Cell cell) {
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue().trim();
        if (cell.getCellType() == CellType.NUMERIC) {
            double v = cell.getNumericCellValue();
            if (v == Math.floor(v)) return String.valueOf((long) v);
            return String.valueOf(v);
        }
        return "";
    }

    private String auditor() {
        var a = SecurityContextHolder.getContext().getAuthentication();
        return a == null ? "system" : a.getName();
    }
}
