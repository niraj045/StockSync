package com.stocksync.migration;

import com.stocksync.migration.parser.SteelfabStockSnapshotParser;
import org.apache.poi.ss.usermodel.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;

/**
 * Reproducible one-time client artifact generator. It preserves Sheet1 exactly
 * and adds human mapping worksheets; production imports still use the controlled
 * Apache POI parser.
 */
public final class PreparedClientWorkbookGenerator {
    private static final Set<String> AMBIGUOUS = Set.of("2", "3", "33", "35");

    private PreparedClientWorkbookGenerator() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Output XLSX path is required");
        byte[] source;
        try (InputStream input = PreparedClientWorkbookGenerator.class
                .getResourceAsStream("/client-data/Stock of material as on 25-07-26.xlsx")) {
            if (input == null) throw new IllegalStateException("Original client workbook fixture is missing");
            source = input.readAllBytes();
        }

        var parsed = new SteelfabStockSnapshotParser().parse(new ByteArrayInputStream(source));
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(source))) {
            removeIfPresent(workbook, "StockSync Item Mapping");
            removeIfPresent(workbook, "StockSync Location Mapping");
            removeIfPresent(workbook, "Import Read Me");
            CellStyle header = headerStyle(workbook);
            CellStyle warning = warningStyle(workbook);

            Sheet itemSheet = workbook.createSheet("StockSync Item Mapping");
            String[] itemHeaders = {
                    "Source Sr.No.", "Exact Source Item Name", "Suggested Item Code", "Normalized Suggestion",
                    "Decision", "Mapped Item Code", "Save Alias", "Explicit Confirmation", "Notes"
            };
            writeHeader(itemSheet, itemHeaders, header);
            int itemRow = 1;
            for (var sourceItem : parsed.sourceItems()) {
                Row row = itemSheet.createRow(itemRow++);
                row.createCell(0).setCellValue(sourceItem.sourceSrNumber());
                row.createCell(1).setCellValue(sourceItem.sourceItemName());
                row.createCell(2).setCellValue("MAT-" + String.format(Locale.ROOT, "%03d",
                        Integer.parseInt(sourceItem.sourceSrNumber())));
                row.createCell(3).setCellValue(sourceItem.normalizedSuggestion());
                row.createCell(4).setCellValue("");
                row.createCell(5).setCellValue("");
                row.createCell(6).setCellValue("");
                row.createCell(7).setCellValue("");
                String note = note(sourceItem.sourceSrNumber());
                row.createCell(8).setCellValue(note);
                if (AMBIGUOUS.contains(sourceItem.sourceSrNumber())) {
                    for (Cell cell : row) cell.setCellStyle(warning);
                }
            }
            setWidths(itemSheet, 14, 30, 20, 28, 22, 20, 14, 22, 70);
            itemSheet.createFreezePane(0, 1);
            itemSheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, itemRow - 1, 0, itemHeaders.length - 1));

            Sheet locationSheet = workbook.createSheet("StockSync Location Mapping");
            String[] locationHeaders = {
                    "Source Column", "Exact Source Location", "Party Decision", "Mapped Party ID",
                    "Mapped Party Name", "Site Decision", "Mapped Site ID", "Mapped Site Name", "Notes"
            };
            writeHeader(locationSheet, locationHeaders, header);
            int locationRow = 1;
            for (var entry : parsed.locations().entrySet()) {
                Row row = locationSheet.createRow(locationRow++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue());
                row.createCell(2).setCellValue("");
                row.createCell(3).setCellValue("");
                row.createCell(4).setCellValue("");
                row.createCell(5).setCellValue("");
                row.createCell(6).setCellValue("");
                row.createCell(7).setCellValue("");
                row.createCell(8).setCellValue("Confirm whether this source label is a legal party, site, or project.");
            }
            setWidths(locationSheet, 14, 28, 20, 18, 28, 20, 18, 28, 65);
            locationSheet.createFreezePane(0, 1);
            locationSheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    0, locationRow - 1, 0, locationHeaders.length - 1));

            Sheet readMe = workbook.createSheet("Import Read Me");
            String[][] facts = {
                    {"StockSync source format", SteelfabStockSnapshotParser.SOURCE_FORMAT},
                    {"Authoritative source cells", "Item names B; party/site quantities C:R; godown quantities U"},
                    {"Ignored source totals", "T and V are not authoritative. T omits P:R."},
                    {"Party/site snapshot date", parsed.partySnapshotDate().toString()},
                    {"Godown snapshot date", parsed.godownSnapshotDate().toString()},
                    {"Source party total (incorrect)", "18,219"},
                    {"Correct party/site total", parsed.partyTotal().toPlainString()},
                    {"Godown total", parsed.godownTotal().toPlainString()},
                    {"Source combined total (incorrect)", "43,601"},
                    {"Correct combined total", parsed.combinedTotal().toPlainString()},
                    {"Understatement from P:R", "2,418"},
                    {"Important warning", "The combined total is not a same-date physical stock count."},
                    {"Workflow", "Upload → map items → map all 16 party/site columns → validate → dry run → ADMIN post"},
                    {"Opening balance meaning", "Godown becomes AVAILABLE; party/site becomes ISSUED. No purchase, order, challan, or rental is created."},
            };
            int factRow = 0;
            for (String[] fact : facts) {
                Row row = readMe.createRow(factRow++);
                row.createCell(0).setCellValue(fact[0]);
                row.createCell(1).setCellValue(fact[1]);
                if (factRow == 1) {
                    row.getCell(0).setCellStyle(header);
                    row.getCell(1).setCellStyle(header);
                }
            }
            setWidths(readMe, 34, 110);

            Path output = Path.of(args[0]).toAbsolutePath().normalize();
            Files.createDirectories(output.getParent());
            try (OutputStream stream = Files.newOutputStream(output)) {
                workbook.write(stream);
            }
            try (InputStream generated = Files.newInputStream(output)) {
                var verified = new SteelfabStockSnapshotParser().parse(generated);
                if (verified.sourceItems().size() != 42 || verified.balances().size() != 152
                        || verified.partyTotal().compareTo(parsed.partyTotal()) != 0
                        || verified.godownTotal().compareTo(parsed.godownTotal()) != 0) {
                    throw new IllegalStateException("Generated workbook no longer matches the controlled source totals");
                }
            }
            System.out.printf("Prepared workbook written: %s (%d source items, %d balances)%n",
                    output, parsed.sourceItems().size(), parsed.balances().size());
        }
    }

    private static String note(String sourceSr) {
        return switch (sourceSr) {
            case "2" -> "Out size H frame: explicitly choose a separate item/variant or exclude pending clarification.";
            case "3" -> "Damage H frame: explicitly choose damaged handling if supported, create a separate item, or exclude.";
            case "33", "35" -> "Duplicate source name shared by Sr.No. 33 and 35. Do not merge automatically.";
            default -> "";
        };
    }

    private static void removeIfPresent(Workbook workbook, String name) {
        int index = workbook.getSheetIndex(name);
        if (index >= 0) workbook.removeSheetAt(index);
    }

    private static void writeHeader(Sheet sheet, String[] values, CellStyle style) {
        Row row = sheet.createRow(0);
        for (int column = 0; column < values.length; column++) {
            Cell cell = row.createCell(column);
            cell.setCellValue(values[column]);
            cell.setCellStyle(style);
        }
    }

    private static CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        return style;
    }

    private static CellStyle warningStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        return style;
    }

    private static void setWidths(Sheet sheet, int... characters) {
        for (int column = 0; column < characters.length; column++) {
            sheet.setColumnWidth(column, Math.min(characters[column], 120) * 256);
        }
    }
}
