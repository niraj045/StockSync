package com.stocksync.migration.parser;

import com.stocksync.common.exception.BusinessRuleException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

@Component
public class LedgerImportParser {

    public record LedgerItemTotal(String itemName, BigDecimal totalDelivered) {}

    public List<LedgerItemTotal> parse(InputStream input) {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheet("D & R");
            if (sheet == null) {
                sheet = workbook.getSheetAt(0);
            }

            // Find header row (usually contains "H frame", "Bracing", etc.)
            // We'll look for a row that has "Date" or "Challan no."
            Row headerRow = null;
            int itemStartCol = -1;
            
            for (int i = 0; i < Math.min(20, sheet.getLastRowNum()); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null && cell.getCellType() == CellType.STRING) {
                        String text = cell.getStringCellValue().trim().toLowerCase();
                        if (text.contains("h frame") || text.contains("date") || text.contains("bracing")) {
                            headerRow = row;
                            break;
                        }
                    }
                }
                if (headerRow != null) break;
            }

            if (headerRow == null) {
                throw new BusinessRuleException("INVALID_LEDGER", "Could not find a header row with items (e.g. H frame, Bracing)");
            }

            // Identify item columns
            Map<Integer, String> columnToItem = new HashMap<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                if (cell != null && cell.getCellType() == CellType.STRING) {
                    String header = cell.getStringCellValue().trim();
                    if (!header.isBlank() && !header.equalsIgnoreCase("Sr. No.") && 
                        !header.equalsIgnoreCase("Challan no.") && !header.toLowerCase().contains("date")) {
                        columnToItem.put(c, header);
                    }
                }
            }

            if (columnToItem.isEmpty()) {
                throw new BusinessRuleException("INVALID_LEDGER", "Could not identify any material columns in the header row");
            }

            // Find "Total Material Delivered" row or sum it manually
            Map<String, BigDecimal> totals = new HashMap<>();
            for (String item : columnToItem.values()) {
                totals.put(item, BigDecimal.ZERO);
            }

            boolean foundTotalRow = false;
            for (int r = headerRow.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                
                // Check if this is the total row
                boolean isTotalRow = false;
                for (int c = 0; c < row.getLastCellNum() && c < 5; c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null && cell.getCellType() == CellType.STRING && 
                        cell.getStringCellValue().toLowerCase().contains("total material delivered")) {
                        isTotalRow = true;
                        break;
                    }
                }

                if (isTotalRow) {
                    foundTotalRow = true;
                    for (Map.Entry<Integer, String> entry : columnToItem.entrySet()) {
                        Cell cell = row.getCell(entry.getKey());
                        if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                            totals.put(entry.getValue(), BigDecimal.valueOf(cell.getNumericCellValue()));
                        }
                    }
                    break; // Stop after finding the total row
                } else {
                    // Accumulate totals manually if no total row is found yet
                    for (Map.Entry<Integer, String> entry : columnToItem.entrySet()) {
                        Cell cell = row.getCell(entry.getKey());
                        if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                            BigDecimal qty = BigDecimal.valueOf(cell.getNumericCellValue());
                            totals.put(entry.getValue(), totals.get(entry.getValue()).add(qty));
                        }
                    }
                }
            }

            List<LedgerItemTotal> result = new ArrayList<>();
            for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
                if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                    result.add(new LedgerItemTotal(entry.getKey(), entry.getValue()));
                }
            }

            return result;
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessRuleException("IMPORT_PARSE_FAILED", "Unable to parse ledger workbook: " + e.getMessage());
        }
    }
}
