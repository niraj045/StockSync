package com.stocksync.reporting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksync.reporting.dto.ReportDtos.ReportFilterRequest;
import com.stocksync.reporting.service.ReportExportService;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportExportWorkbookTest {
    @Test
    void excelUsesSteelFabFormatAndPreservesTypedValues() throws Exception {
        ReportExportService service = new ReportExportService(null, null, new ObjectMapper(), null, "target/test-exports");
        ReportFilterRequest filters = new ReportFilterRequest(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31), 7L, null, null,
                null, null, "ACTIVE", null, null, null, 0, 25);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("document_number", "AGR/2026/0001");
        row.put("effective_date", LocalDate.of(2026, 8, 2));
        row.put("grand_total", new BigDecimal("12345.67"));

        Method excel = ReportExportService.class.getDeclaredMethod("excel", String.class, ReportFilterRequest.class, String.class, List.class);
        excel.setAccessible(true);
        byte[] bytes = (byte[]) excel.invoke(service, "AGREEMENT_REGISTER", filters, "4m Facade LLP", List.of(row));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheetAt(0);
            assertTrue(sheet.getSheetName().startsWith("Agreement Register - 4m"));
            assertEquals("SteelFab Scaffoldings & Engineering Pvt. Ltd.", sheet.getRow(0).getCell(1).getStringCellValue());
            assertTrue(sheet.getRow(1).getCell(1).getStringCellValue().contains("4m Facade LLP"));
            assertTrue(sheet.getRow(3).getCell(1).getStringCellValue().contains("Company 4m Facade LLP"));
            assertEquals("Document Number", sheet.getRow(5).getCell(0).getStringCellValue());
            assertEquals(CellType.NUMERIC, sheet.getRow(6).getCell(1).getCellType());
            assertEquals(CellType.NUMERIC, sheet.getRow(6).getCell(2).getCellType());
            assertEquals(12345.67, sheet.getRow(6).getCell(2).getNumericCellValue(), 0.001);
            assertNotNull(sheet.getPaneInformation());
            assertTrue(sheet.getPaneInformation().isFreezePane());
            assertNotNull(sheet.getCTWorksheet().getAutoFilter());
            assertEquals(0, sheet.getPrintSetup().getFitHeight());
        }
    }

    @Test
    void issuedChallanExcelKeepsOriginalFieldsAndAddsItemsColumn() throws Exception {
        ReportExportService service = new ReportExportService(null, null, new ObjectMapper(), null, "target/test-exports");
        ReportFilterRequest filters = new ReportFilterRequest(
                null, null, 7L, null, null, null, null, null, null, null, null, 0, 25);

        Map<String, Object> challan = new LinkedHashMap<>();
        challan.put("document_number", "IC/2026-27/0001");
        challan.put("date", LocalDate.of(2026, 8, 18));
        challan.put("party", "Demo Company Pvt. Ltd.");
        challan.put("site", "Demo Site");
        challan.put("vehicle", "MH04AB1234");
        challan.put("driver", "Rajendra");
        challan.put("items", "H frames (35 NOS); Bracing (20 NOS)");
        challan.put("issued_quantity", new BigDecimal("35"));
        challan.put("weight", new BigDecimal("120.50"));
        challan.put("status", "FULFILLED");

        Method excel = ReportExportService.class.getDeclaredMethod(
                "excel", String.class, ReportFilterRequest.class, String.class, List.class);
        excel.setAccessible(true);
        byte[] bytes = (byte[]) excel.invoke(service, "ISSUED_CHALLANS_REGISTER", filters, "Demo Company Pvt. Ltd.",
                List.of(challan));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheetAt(0);
            String[] expectedHeaders = {"Document Number", "Date", "Party", "Site", "Vehicle", "Driver",
                    "Items", "Issued Quantity", "Weight", "Status"};
            for (int column = 0; column < expectedHeaders.length; column++) {
                assertEquals(expectedHeaders[column], sheet.getRow(5).getCell(column).getStringCellValue());
            }
            assertEquals("H frames (35 NOS); Bracing (20 NOS)", sheet.getRow(6).getCell(6).getStringCellValue());
            assertTrue(sheet.getRow(6).getCell(6).getCellStyle().getWrapText());
            assertEquals(35, sheet.getRow(6).getCell(7).getNumericCellValue(), 0.001);
            assertEquals("FULFILLED", sheet.getRow(6).getCell(9).getStringCellValue());
            assertNull(sheet.getRow(9));
        }
    }

    @Test
    void receivingChallanExcelKeepsOriginalFieldsAndAddsItemsColumn() throws Exception {
        ReportExportService service = new ReportExportService(null, null, new ObjectMapper(), null, "target/test-exports");
        ReportFilterRequest filters = new ReportFilterRequest(
                null, null, 7L, null, null, null, null, null, null, null, null, 0, 25);

        Map<String, Object> challan = new LinkedHashMap<>();
        challan.put("document_number", "RC/2026-27/0001");
        challan.put("date", LocalDate.of(2026, 8, 19));
        challan.put("party", "Demo Company Pvt. Ltd.");
        challan.put("site", "Demo Site");
        challan.put("vehicle", "MH04AB1234");
        challan.put("driver", "Rajendra");
        challan.put("items", "H frames (Good 25 NOS, Damaged 2 NOS, Lost 1 NOS)");
        challan.put("status", "POSTED");
        challan.put("received_quantity", new BigDecimal("28"));
        challan.put("damaged_quantity", new BigDecimal("2"));
        challan.put("lost_quantity", new BigDecimal("1"));

        Method excel = ReportExportService.class.getDeclaredMethod(
                "excel", String.class, ReportFilterRequest.class, String.class, List.class);
        excel.setAccessible(true);
        byte[] bytes = (byte[]) excel.invoke(service, "RECEIVING_CHALLANS_REGISTER", filters, "Demo Company Pvt. Ltd.",
                List.of(challan));

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheetAt(0);
            String[] expectedHeaders = {"Document Number", "Date", "Party", "Site", "Vehicle", "Driver",
                    "Items", "Status", "Received Quantity", "Damaged Quantity", "Lost Quantity"};
            for (int column = 0; column < expectedHeaders.length; column++) {
                assertEquals(expectedHeaders[column], sheet.getRow(5).getCell(column).getStringCellValue());
            }
            assertEquals("H frames (Good 25 NOS, Damaged 2 NOS, Lost 1 NOS)",
                    sheet.getRow(6).getCell(6).getStringCellValue());
            assertTrue(sheet.getRow(6).getCell(6).getCellStyle().getWrapText());
            assertTrue(sheet.getRow(6).getHeightInPoints() > 15f);
            assertEquals("POSTED", sheet.getRow(6).getCell(7).getStringCellValue());
            assertEquals(28, sheet.getRow(6).getCell(8).getNumericCellValue(), 0.001);
            assertNull(sheet.getRow(9));
        }
    }

}
