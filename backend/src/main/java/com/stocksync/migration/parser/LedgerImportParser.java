package com.stocksync.migration.parser;

import com.stocksync.common.exception.BusinessRuleException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * Parser for the SBUT D&R (Delivered & Returned) Excel format.
 *
 * Expected sheet layout (sheet tab named "D & R"):
 *   Row N:   [Site Name]                          ← merged cells
 *   Row N+1: "Statement of Material Delivered & Returned"  ← format identifier
 *   Row N+2: Sr. No. | Challan no. | Date | H frame | Bracing | ... ← column headers
 *   Row N+3..M: challan data rows
 *   Row M+1: "Total Material Delivered" | ... totals
 */
@Component
public class LedgerImportParser {

    private static final String FORMAT_IDENTIFIER = "statement of material delivered";
    private static final String TOTAL_ROW_MARKER  = "total material delivered";

    /** Parsed result including site name detected from the file. */
    public record LedgerParseResult(String detectedSiteName, List<LedgerItemTotal> totals) {}

    /** Per-item total as parsed from the Total Material Delivered row. */
    public record LedgerItemTotal(String itemName, BigDecimal totalDelivered) {}

    // ── public API ──────────────────────────────────────────────────────────

    /**
     * Parse with auto-detection of site name.
     * Validates that the file is in the correct SBUT D&R format.
     */
    public LedgerParseResult parseWithSiteName(InputStream input) {
        try (Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = findDRSheet(workbook);
            return parseSheet(sheet);
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessRuleException("IMPORT_PARSE_FAILED",
                    "Unable to parse D&R workbook: " + e.getMessage());
        }
    }

    /**
     * Backward-compatible: parse without site name (site must be supplied externally).
     */
    public List<LedgerItemTotal> parse(InputStream input) {
        return parseWithSiteName(input).totals();
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private Sheet findDRSheet(Workbook wb) {
        // Prefer the tab literally named "D & R"
        Sheet sheet = wb.getSheet("D & R");
        if (sheet == null) sheet = wb.getSheet("D&R");
        if (sheet == null) sheet = wb.getSheet("D & R ");
        if (sheet == null) {
            // Scan all sheets for the format identifier
            for (int i = 0; i < wb.getNumberOfSheets(); i++) {
                Sheet s = wb.getSheetAt(i);
                if (containsFormatIdentifier(s)) {
                    sheet = s;
                    break;
                }
            }
        }
        if (sheet == null) {
            throw new BusinessRuleException("INVALID_DR_FORMAT",
                    "No 'D & R' sheet found. Please upload a valid SBUT D&R Excel file.");
        }
        return sheet;
    }

    private boolean containsFormatIdentifier(Sheet sheet) {
        for (int i = 0; i < Math.min(15, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                String txt = cellText(row, c);
                if (txt.toLowerCase().contains(FORMAT_IDENTIFIER)) return true;
            }
        }
        return false;
    }

    private LedgerParseResult parseSheet(Sheet sheet) {
        // ── Step 1: Find the "Statement of Material…" identifier row ────────
        int identifierRowIdx = -1;
        for (int i = 0; i < Math.min(20, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            for (int c = 0; c < row.getLastCellNum(); c++) {
                if (cellText(row, c).toLowerCase().contains(FORMAT_IDENTIFIER)) {
                    identifierRowIdx = i;
                    break;
                }
            }
            if (identifierRowIdx >= 0) break;
        }

        if (identifierRowIdx < 0) {
            throw new BusinessRuleException("INVALID_DR_FORMAT",
                    "Missing 'Statement of Material Delivered & Returned' header. " +
                    "Please upload a valid SBUT D&R Excel file.");
        }

        // ── Step 2: Site name is in the row immediately above the identifier ─
        String siteName = "";
        if (identifierRowIdx > 0) {
            Row siteNameRow = sheet.getRow(identifierRowIdx - 1);
            if (siteNameRow != null) {
                for (int c = 0; c < siteNameRow.getLastCellNum(); c++) {
                    String txt = cellText(siteNameRow, c).trim();
                    if (!txt.isBlank()) {
                        siteName = txt;
                        break;
                    }
                }
            }
        }
        if (siteName.isBlank()) {
            throw new BusinessRuleException("INVALID_DR_FORMAT",
                    "Could not detect site name. The row above 'Statement of Material Delivered & Returned' must contain the site name.");
        }

        // ── Step 3: Column-header row is immediately after the identifier ────
        int headerRowIdx = identifierRowIdx + 1;
        Row headerRow = sheet.getRow(headerRowIdx);
        if (headerRow == null) {
            throw new BusinessRuleException("INVALID_DR_FORMAT",
                    "Could not find column header row after format identifier.");
        }

        // Build column-index → item name map (skip metadata columns)
        Map<Integer, String> colToItem = new LinkedHashMap<>();
        for (int c = 0; c < headerRow.getLastCellNum(); c++) {
            String header = cellText(headerRow, c).trim();
            if (header.isBlank()) continue;
            String lc = header.toLowerCase();
            if (lc.contains("sr") || lc.contains("challan") || lc.contains("date")
                    || lc.equals("no.") || lc.equals("no")) continue;
            colToItem.put(c, header);
        }
        if (colToItem.isEmpty()) {
            throw new BusinessRuleException("INVALID_DR_FORMAT",
                    "No material columns found in the header row.");
        }

        // ── Step 4: Scan data rows — prefer "Total Material Delivered" row ──
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        colToItem.values().forEach(n -> totals.put(n, BigDecimal.ZERO));

        boolean foundTotalRow = false;
        for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            // Check first few cells for the total-row marker
            boolean isTotalRow = false;
            for (int c = 0; c < Math.min(5, row.getLastCellNum()); c++) {
                if (cellText(row, c).toLowerCase().contains(TOTAL_ROW_MARKER)) {
                    isTotalRow = true;
                    break;
                }
            }

            if (isTotalRow) {
                // Read totals from this row
                for (Map.Entry<Integer, String> entry : colToItem.entrySet()) {
                    Cell cell = row.getCell(entry.getKey());
                    if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                        totals.put(entry.getValue(), BigDecimal.valueOf(cell.getNumericCellValue()));
                    }
                }
                foundTotalRow = true;
                break;
            }

            // Accumulate per-row quantities (fallback if no total row)
            for (Map.Entry<Integer, String> entry : colToItem.entrySet()) {
                Cell cell = row.getCell(entry.getKey());
                if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                    totals.merge(entry.getValue(),
                            BigDecimal.valueOf(cell.getNumericCellValue()), BigDecimal::add);
                }
            }
        }

        if (!foundTotalRow) {
            // Not necessarily an error — accumulated sum is the fallback
            // but log a warning (actual logging omitted to keep it simple)
        }

        // Build result list (only non-zero items)
        List<LedgerItemTotal> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> e : totals.entrySet()) {
            if (e.getValue().compareTo(BigDecimal.ZERO) > 0) {
                result.add(new LedgerItemTotal(e.getKey(), e.getValue()));
            }
        }

        return new LedgerParseResult(siteName, result);
    }

    /** Safe cell text extractor — handles STRING and other types. */
    private String cellText(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) return cell.getStringCellValue();
        if (cell.getCellType() == CellType.NUMERIC) return String.valueOf((long) cell.getNumericCellValue());
        return "";
    }
}
