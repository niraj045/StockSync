package com.stocksync.reporting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.reporting.dto.ReportDtos.ExportFormat;
import com.stocksync.reporting.dto.ReportDtos.ExportHistoryResponse;
import com.stocksync.reporting.dto.ReportDtos.ReportExportResponse;
import com.stocksync.reporting.dto.ReportDtos.ReportFilterRequest;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportExportService {
    private final ReportQueryService queries;
    private final ReportCatalogService catalog;
    private final ObjectMapper mapper;
    private final JdbcTemplate jdbc;
    private final Path exportRoot;

    public ReportExportService(ReportQueryService queries, ReportCatalogService catalog, ObjectMapper mapper, JdbcTemplate jdbc,
                               @Value("${stocksync.file-storage-path}") String storageRoot) {
        this.queries = queries;
        this.catalog = catalog;
        this.mapper = mapper;
        this.jdbc = jdbc;
        this.exportRoot = Paths.get(storageRoot).toAbsolutePath().normalize().resolve("report-exports").normalize();
    }

    @Transactional
    public ReportExportResponse export(String reportType, ReportFilterRequest filters, ExportFormat format, Authentication auth, HttpServletRequest request) {
        String type = catalog.normalize(reportType);
        catalog.require(type, auth);
        List<Map<String, Object>> rows = queries.exportRows(type, filters);
        String companyName = companyName(filters);
        String companySuffix = companyName == null ? "" : "-" + safe(companyName);
        String filename = safe(type) + companySuffix + "-" + LocalDate.now() + "." + extension(format);
        try {
            Files.createDirectories(exportRoot);
            Path file = exportRoot.resolve(Instant.now().toEpochMilli() + "-" + filename).normalize();
            if (!file.startsWith(exportRoot)) throw new BusinessRuleException("INVALID_EXPORT_PATH", "Invalid export path");
            byte[] bytes = switch (format) {
                case CSV -> csv(rows);
                case EXCEL -> excel(type, filters, companyName, rows);
                case PDF -> pdf(type, rows);
            };
            Files.write(file, bytes);
            Long id = insertHistory(type, filters, format, auth, filename, exportRoot.relativize(file).toString(), contentType(format), bytes.length, "SUCCESS", null);
            return new ReportExportResponse(id, type, format, filename, "SUCCESS", Instant.now());
        } catch (Exception e) {
            Long id = insertHistory(type, filters, format, auth, filename, null, null, null, "FAILED", e.getMessage());
            throw new BusinessRuleException("REPORT_EXPORT_FAILED", "Unable to export report " + id + ": " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<ExportHistoryResponse> history(Authentication auth) {
        boolean admin = has(auth, "ROLE_ADMIN");
        String sql = """
                SELECT id, report_type, export_format, original_filename, status, error_message, generated_by_username, generated_at, file_size
                FROM report_export_history
                """ + (admin ? "" : " WHERE generated_by_username = ? ") + " ORDER BY generated_at DESC LIMIT 50";
        Object[] args = admin ? new Object[]{} : new Object[]{username(auth)};
        return jdbc.query(sql, (rs, rowNum) -> new ExportHistoryResponse(
                rs.getLong("id"),
                rs.getString("report_type"),
                ExportFormat.valueOf(rs.getString("export_format")),
                rs.getString("original_filename"),
                rs.getString("status"),
                rs.getString("error_message"),
                rs.getString("generated_by_username"),
                rs.getTimestamp("generated_at").toInstant(),
                rs.getObject("file_size") == null ? null : rs.getLong("file_size")
        ), args);
    }

    @Transactional(readOnly = true)
    public Download download(Long id, Authentication auth) {
        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM report_export_history WHERE id = ?", id);
        if (!has(auth, "ROLE_ADMIN") && !username(auth).equals(row.get("generated_by_username"))) {
            throw new org.springframework.security.access.AccessDeniedException("Export not permitted");
        }
        if (!"SUCCESS".equals(row.get("status")) || row.get("storage_path") == null) {
            throw new BusinessRuleException("REPORT_EXPORT_NOT_AVAILABLE", "Export file is not available");
        }
        Path file = exportRoot.resolve(row.get("storage_path").toString()).normalize();
        if (!file.startsWith(exportRoot) || !Files.isRegularFile(file)) {
            throw new BusinessRuleException("REPORT_EXPORT_NOT_FOUND", "Export file not found");
        }
        return new Download(new FileSystemResource(file), row.get("original_filename").toString(), row.get("content_type").toString());
    }

    private Long insertHistory(String type, ReportFilterRequest filters, ExportFormat format, Authentication auth, String filename, String path, String contentType, Integer size, String status, String error) {
        jdbc.update("""
                INSERT INTO report_export_history(report_type, export_format, filter_json, generated_by_user_id, generated_by_username, storage_path, original_filename, content_type, file_size, status, error_message)
                VALUES (?, ?, ?, NULL, ?, ?, ?, ?, ?, ?, ?)
                """, type, format.name(), json(filters), username(auth), path, filename, contentType, size, status, error);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private byte[] csv(List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return "No data\n".getBytes(StandardCharsets.UTF_8);
        StringBuilder out = new StringBuilder();
        List<String> headers = List.copyOf(rows.getFirst().keySet());
        out.append(String.join(",", headers)).append('\n');
        for (Map<String, Object> row : rows) {
            out.append(headers.stream().map(h -> escape(row.get(h))).collect(java.util.stream.Collectors.joining(","))).append('\n');
        }
        return out.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] excel(String type, ReportFilterRequest filters, String companyName, List<Map<String, Object>> rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            String reportName = displayHeader(type) + (companyName == null ? "" : " - " + companyName);
            var sheet = workbook.createSheet(WorkbookUtil.createSafeSheetName(reportName));
            sheet.setDisplayGridlines(false);
            sheet.setAutobreaks(true);
            sheet.getPrintSetup().setLandscape(true);
            sheet.getPrintSetup().setFitWidth((short) 1);
            sheet.setFitToPage(true);

            CellStyle companyStyle = titleStyle(workbook, 16, IndexedColors.DARK_TEAL, true);
            CellStyle reportStyle = titleStyle(workbook, 13, IndexedColors.BLACK, true);
            CellStyle metadataStyle = titleStyle(workbook, 9, IndexedColors.GREY_50_PERCENT, false);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle textStyle = bodyStyle(workbook, false);
            CellStyle alternateStyle = bodyStyle(workbook, true);
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(textStyle);
            numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00##"));
            CellStyle alternateNumberStyle = workbook.createCellStyle();
            alternateNumberStyle.cloneStyleFrom(alternateStyle);
            alternateNumberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00##"));
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.cloneStyleFrom(textStyle);
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("dd-mmm-yyyy"));
            CellStyle alternateDateStyle = workbook.createCellStyle();
            alternateDateStyle.cloneStyleFrom(alternateStyle);
            alternateDateStyle.setDataFormat(workbook.createDataFormat().getFormat("dd-mmm-yyyy"));

            int columnCount = rows.isEmpty() ? 1 : rows.getFirst().size();
            int mergeEnd = Math.max(5, columnCount - 1);
            addLetterhead(workbook, sheet);
            Row company = sheet.createRow(0);
            company.setHeightInPoints(28);
            Cell companyCell = company.createCell(1);
            companyCell.setCellValue("SteelFab Scaffoldings & Engineering Pvt. Ltd.");
            companyCell.setCellStyle(companyStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 1, mergeEnd));

            Row report = sheet.createRow(1);
            Cell reportCell = report.createCell(1);
            reportCell.setCellValue(reportName);
            reportCell.setCellStyle(reportStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, mergeEnd));

            Row generated = sheet.createRow(2);
            Cell generatedCell = generated.createCell(1);
            generatedCell.setCellValue("Generated: " + LocalDateTime.now(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
            generatedCell.setCellStyle(metadataStyle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 1, mergeEnd));

            Row filterRow = sheet.createRow(3);
            Cell filterCell = filterRow.createCell(1);
            filterCell.setCellValue(filterSummary(filters, companyName));
            filterCell.setCellStyle(metadataStyle);
            sheet.addMergedRegion(new CellRangeAddress(3, 3, 1, mergeEnd));

            int headerIndex = 5;
            if (rows.isEmpty()) {
                Cell empty = sheet.createRow(headerIndex).createCell(0);
                empty.setCellValue("No records match the selected filters.");
                empty.setCellStyle(textStyle);
            } else {
                List<String> headers = List.copyOf(rows.getFirst().keySet());
                Row header = sheet.createRow(headerIndex);
                header.setHeightInPoints(28);
                for (int i = 0; i < headers.size(); i++) {
                    Cell cell = header.createCell(i);
                    cell.setCellValue(displayHeader(headers.get(i)));
                    cell.setCellStyle(headerStyle);
                }
                for (int r = 0; r < rows.size(); r++) {
                    Row row = sheet.createRow(headerIndex + r + 1);
                    boolean alternate = r % 2 == 1;
                    for (int c = 0; c < headers.size(); c++) {
                        Cell cell = row.createCell(c);
                        setCell(cell, rows.get(r).get(headers.get(c)), alternate, textStyle, alternateStyle,
                                numberStyle, alternateNumberStyle, dateStyle, alternateDateStyle);
                    }
                }
                sheet.createFreezePane(0, headerIndex + 1);
                sheet.setAutoFilter(new CellRangeAddress(headerIndex, headerIndex + rows.size(), 0, headers.size() - 1));
                sheet.setRepeatingRows(CellRangeAddress.valueOf("$" + (headerIndex + 1) + ":$" + (headerIndex + 1)));
                for (int i = 0; i < Math.min(headers.size(), 30); i++) {
                    sheet.autoSizeColumn(i);
                    sheet.setColumnWidth(i, Math.min(Math.max(sheet.getColumnWidth(i) + 768, 3200), 12000));
                }
            }
            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void addLetterhead(XSSFWorkbook workbook, org.apache.poi.xssf.usermodel.XSSFSheet sheet) {
        try {
            var resource = new ClassPathResource("pdf-assets/steelfab-letterhead.jpg");
            if (!resource.exists()) return;
            int picture = workbook.addPicture(resource.getInputStream().readAllBytes(), XSSFWorkbook.PICTURE_TYPE_JPEG);
            var anchor = workbook.getCreationHelper().createClientAnchor();
            anchor.setCol1(0);
            anchor.setRow1(0);
            anchor.setCol2(1);
            anchor.setRow2(4);
            sheet.createDrawingPatriarch().createPicture(anchor, picture);
            sheet.setColumnWidth(0, 2800);
        } catch (IOException ignored) {
            // Branding text remains available if the packaged artwork cannot be read.
        }
    }

    private CellStyle titleStyle(XSSFWorkbook workbook, int size, IndexedColors color, boolean bold) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Aptos");
        font.setFontHeightInPoints((short) size);
        font.setBold(bold);
        font.setColor(color.getIndex());
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle headerStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Aptos");
        font.setFontHeightInPoints((short) 10);
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private CellStyle bodyStyle(XSSFWorkbook workbook, boolean alternate) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setFontName("Aptos");
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        if (alternate) {
            style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return style;
    }

    private String filterSummary(ReportFilterRequest filters, String companyName) {
        List<String> values = new ArrayList<>();
        if (filters.startDate() != null) values.add("From " + filters.startDate());
        if (filters.endDate() != null) values.add("To " + filters.endDate());
        if (filters.month() != null) values.add("Month " + filters.month());
        if (filters.partyId() != null) values.add("Company " + (companyName == null ? filters.partyId() : companyName));
        if (filters.siteId() != null) values.add("Site ID " + filters.siteId());
        if (filters.agreementId() != null) values.add("Agreement ID " + filters.agreementId());
        if (filters.itemId() != null) values.add("Item ID " + filters.itemId());
        if (filters.categoryId() != null) values.add("Category ID " + filters.categoryId());
        if (filters.status() != null && !filters.status().isBlank()) values.add("Status " + filters.status());
        if (filters.documentNumber() != null && !filters.documentNumber().isBlank()) values.add("Document " + filters.documentNumber());
        return values.isEmpty() ? "Filters: All records" : "Filters: " + String.join(" | ", values);
    }

    private String companyName(ReportFilterRequest filters) {
        if (filters == null || filters.partyId() == null) return null;
        return jdbc.query(
                "SELECT legal_name FROM parties WHERE id = ?",
                (rs, rowNum) -> rs.getString("legal_name"),
                filters.partyId()
        ).stream().findFirst().orElse(null);
    }

    private String displayHeader(String header) {
        String[] words = header.replace('_', ' ').trim().split("\\s+");
        List<String> display = new ArrayList<>();
        for (String word : words) {
            String lower = word.toLowerCase(Locale.ROOT);
            display.add(lower.isEmpty() ? lower : Character.toUpperCase(lower.charAt(0)) + lower.substring(1));
        }
        return String.join(" ", display);
    }

    private byte[] pdf(String type, List<Map<String, Object>> rows) throws IOException {
        StringBuilder html = new StringBuilder("<html><head><style>body{font-family:Arial,sans-serif;font-size:11px}table{width:100%;border-collapse:collapse}th,td{border:1px solid #ccc;padding:4px}th{background:#eee;text-align:left}</style></head><body>");
        html.append("<h2>").append(type).append("</h2><p>Generated for review. GST preparation exports do not file returns.</p><table>");
        if (rows.isEmpty()) {
            html.append("<tr><td>No data</td></tr>");
        } else {
            List<String> headers = List.copyOf(rows.getFirst().keySet());
            html.append("<tr>");
            headers.forEach(h -> html.append("<th>").append(escapeHtml(h)).append("</th>"));
            html.append("</tr>");
            rows.stream().limit(500).forEach(row -> {
                html.append("<tr>");
                headers.forEach(h -> html.append("<td>").append(escapeHtml(String.valueOf(row.get(h)))).append("</td>"));
                html.append("</tr>");
            });
        }
        html.append("</table></body></html>");
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html.toString(), null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        }
    }

    private void setCell(Cell cell, Object value, boolean alternate, CellStyle textStyle, CellStyle alternateStyle,
                         CellStyle numberStyle, CellStyle alternateNumberStyle, CellStyle dateStyle, CellStyle alternateDateStyle) {
        cell.setCellStyle(alternate ? alternateStyle : textStyle);
        if (value == null) return;
        if (value instanceof BigDecimal bd) {
            cell.setCellValue(bd.doubleValue());
            cell.setCellStyle(alternate ? alternateNumberStyle : numberStyle);
        } else if (value instanceof Number n) {
            cell.setCellValue(n.doubleValue());
            cell.setCellStyle(alternate ? alternateNumberStyle : numberStyle);
        } else if (value instanceof java.sql.Date d) {
            cell.setCellValue(d.toLocalDate());
            cell.setCellStyle(alternate ? alternateDateStyle : dateStyle);
        } else if (value instanceof LocalDate d) {
            cell.setCellValue(d);
            cell.setCellStyle(alternate ? alternateDateStyle : dateStyle);
        } else if (value instanceof java.sql.Timestamp t) cell.setCellValue(t.toInstant().toString());
        else cell.setCellValue(String.valueOf(value));
    }

    private String escape(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) return "\"" + text.replace("\"", "\"\"") + "\"";
        return text;
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String extension(ExportFormat format) {
        return switch (format) { case PDF -> "pdf"; case EXCEL -> "xlsx"; case CSV -> "csv"; };
    }

    private String contentType(ExportFormat format) {
        return switch (format) {
            case PDF -> "application/pdf";
            case EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case CSV -> "text/csv";
        };
    }

    private String safe(String type) {
        return type.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (JsonProcessingException e) { return "{}"; }
    }

    private boolean has(Authentication auth, String role) {
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(role));
    }

    private String username(Authentication auth) {
        return auth == null ? "anonymous" : auth.getName();
    }

    public record Download(Resource resource, String filename, String contentType) {}
}
