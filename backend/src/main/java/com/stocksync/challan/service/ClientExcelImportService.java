package com.stocksync.challan.service;

import com.stocksync.challan.entity.*;
import com.stocksync.challan.repository.IssuedChallanRepository;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.inventory.entity.*;
import com.stocksync.inventory.repository.*;
import com.stocksync.order.entity.SiteOrder;
import com.stocksync.order.repository.SiteOrderRepository;
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

    public ClientExcelImportService(
            IssuedChallanRepository challans,
            SiteOrderRepository orders,
            ItemRepository items,
            StockBalanceRepository balances,
            SiteStockBalanceRepository siteBalances,
            StockTransactionRepository transactions,
            JdbcTemplate jdbc) {
        this.challans = challans;
        this.orders = orders;
        this.items = items;
        this.balances = balances;
        this.siteBalances = siteBalances;
        this.transactions = transactions;
        this.jdbc = jdbc;
    }

    @Transactional
    public int importClientExcel(MultipartFile file) {
        List<Item> allItems = items.findAll();
        int importedCount = 0;
        String actor = auditor();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Find Header Row
            int headerRowIndex = -1;
            int challanNoCol = -1;
            int dateCol = -1;
            
            for (Row row : sheet) {
                for (Cell cell : row) {
                    if (cell.getCellType() == CellType.STRING) {
                        String val = cell.getStringCellValue().trim().toLowerCase();
                        if (val.contains("challan no") || val.contains("challan number")) {
                            headerRowIndex = row.getRowNum();
                            challanNoCol = cell.getColumnIndex();
                        } else if (val.equals("date") && headerRowIndex == row.getRowNum()) {
                            dateCol = cell.getColumnIndex();
                        }
                    }
                }
                if (headerRowIndex != -1 && challanNoCol != -1 && dateCol != -1) break;
            }
            
            if (headerRowIndex == -1) {
                throw new BusinessRuleException("INVALID_FORMAT", "Could not find 'Challan no.' header in the first sheet.");
            }
            
            // Auto-detect Target Site Order
            List<SiteOrder> activeOrders = orders.findAll().stream()
                    .filter(o -> !o.getStatus().name().equals("CANCELLED"))
                    .toList();
            SiteOrder order = null;
            
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
            
            if (order == null) {
                throw new BusinessRuleException("SITE_ORDER_NOT_FOUND", "Could not automatically determine the Site Order from the Excel file contents. Ensure the site name is present above the header row.");
            }
            
            Row headerRow = sheet.getRow(headerRowIndex);
            
            // Map columns to Items
            Map<Integer, Item> columnItemMap = new HashMap<>();
            for (Cell cell : headerRow) {
                if (cell.getColumnIndex() <= dateCol) continue;
                if (cell.getCellType() != CellType.STRING) continue;
                
                String headerName = cell.getStringCellValue().trim();
                if (headerName.isEmpty()) continue;
                
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
                        if (item.getItemName().toLowerCase().contains(headerName.toLowerCase()) || 
                            headerName.toLowerCase().contains(item.getItemName().toLowerCase())) {
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
                c.setSiteOrder(order);
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
                        
                        // Update Balances
                        StockBalance balance = balances.findForUpdate(item.getId()).orElseGet(() -> {
                            jdbc.update("INSERT IGNORE INTO stock_balances(item_id) VALUES(?)", item.getId());
                            return balances.findForUpdate(item.getId()).orElseThrow();
                        });
                        balance.setAvailableQuantity(balance.getAvailableQuantity().subtract(qty));
                        balance.setIssuedQuantity(balance.getIssuedQuantity().add(qty));
                        balances.save(balance);

                        SiteStockBalance siteBalance = siteBalances.findForUpdate(order.getSite().getId(), item.getId()).orElseGet(() -> {
                            jdbc.update("INSERT IGNORE INTO site_stock_balances(site_id, item_id) VALUES(?, ?)", order.getSite().getId(), item.getId());
                            return siteBalances.findForUpdate(order.getSite().getId(), item.getId()).orElseThrow();
                        });
                        siteBalance.setPendingQuantity(siteBalance.getPendingQuantity().add(qty));
                        siteBalances.save(siteBalance);
                        
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
                        txIn.setSite(order.getSite());
                        txIn.setParty(order.getParty());
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
