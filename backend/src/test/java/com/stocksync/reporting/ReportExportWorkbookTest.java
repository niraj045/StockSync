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
        }
    }
}
