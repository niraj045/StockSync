package com.stocksync.quotation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocksync.common.exception.BusinessRuleException;
import com.stocksync.quotation.dto.QuotationItemResponse;
import com.stocksync.quotation.dto.QuotationResponse;
import com.stocksync.quotation.dto.SteelFabExactHireRequest;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class SteelFabExactHirePdfService {
    public static final String TEMPLATE_CODE = "STEELFAB_EXACT_HIRE_V1";
    private static final String PDF_PATH = "pdf-templates/steelfab-exact-hire-v1.pdf";
    private static final String CONFIG_PATH = "pdf-templates/steelfab-exact-hire-v1-fields.json";
    private static final int PRIMARY_TABLE_ROWS = 7;
    private static final int CONTINUATION_TABLE_ROWS = 18;
    private final ExactPdfConfig config;
    private final SteelFabExactHireFormatter format = new SteelFabExactHireFormatter();

    public SteelFabExactHirePdfService(ObjectMapper objectMapper) {
        try (InputStream input = new ClassPathResource(CONFIG_PATH).getInputStream()) {
            this.config = objectMapper.readValue(input, ExactPdfConfig.class);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load SteelFab exact PDF coordinates", e);
        }
    }

    public PdfDocument generate(QuotationResponse quotation) {
        requireExactTemplate(quotation);
        SteelFabExactHireRequest exact = quotation.exactHire();
        if (exact == null) throw new BusinessRuleException("EXACT_HIRE_FIELDS_REQUIRED", "Exact SteelFab PDF details are required");
        if (quotation.items() == null || quotation.items().isEmpty())
            throw new BusinessRuleException("QUOTATION_ITEMS_REQUIRED", "At least one SteelFab material is required");
        try (InputStream source = new ClassPathResource(PDF_PATH).getInputStream();
             PDDocument document = PDDocument.load(source);
             InputStream continuationSource = new ClassPathResource(PDF_PATH).getInputStream();
             PDDocument continuationTemplate = PDDocument.load(continuationSource);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (document.getNumberOfPages() != config.pageCount()) {
                throw new IllegalStateException("SteelFab source PDF must contain exactly five pages");
            }
            stamp(document, quotation, exact, quotation.items());
            appendContinuationSchedules(document, continuationTemplate, quotation.items());
            document.save(output);
            return new PdfDocument(filename(quotation), output.toByteArray(), config.templateVersion(), config.coordinatesVersion());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate SteelFab exact PDF", e);
        }
    }

    private void stamp(PDDocument document, QuotationResponse q, SteelFabExactHireRequest e,
            List<QuotationItemResponse> items) throws IOException {
        draw(document, "referenceNumber", " "+q.quotationNumber());
        draw(document, "quotationDate", " "+format.dated(q.quotationDate()));
        draw(document, "partyName", q.partyName());
        draw(document, "partyAddress", e.partyAddress());
        draw(document, "subject", " "+value(e.subject(), "Quotation for Supply of H frame Scaffolding Materials on Hire for " + q.siteName() + "."));
        int validity = e.validityDays() == null ? 7 : e.validityDays();
        draw(document, "validity", validity + " (" + format.integerWords(validity) + ")");
        String minimumPeriod=value(e.minimumHirePeriod(), "6 Months (180 days)");
        draw(document, "minimumHirePeriod", " "+minimumPeriod);
        draw(document, "minimumHireCommitment", " "+minimumPeriod.replaceFirst("\\s*\\(.*$", "").replaceFirst("\\.*$", "")+".");
        draw(document, "siteMeasurement", "As per the " + q.siteName() + " measurement of " + format.quantity(e.siteLengthRmt())
                + " RMT Length and " + format.quantity(e.siteHeightMtr()) + " MTR Height, the approximate quantity of H-Frame type scaffolding material required for the complete site are as follows:");
        drawRequiredMaterials(document, items);
        drawPrimaryMaterialTable(document, items.subList(0, Math.min(PRIMARY_TABLE_ROWS, items.size())));
        draw(document, "subtotal", format.money(q.subtotal()));
        BigDecimal gst = e.gstPercentage() == null ? q.cgstRate().add(q.sgstRate()).add(q.igstRate()) : e.gstPercentage();
        draw(document, "gstLabel", "GST " + format.quantity(gst) + "%");
        draw(document, "gstAmount", format.money(q.totalTax()));
        draw(document, "grandTotal", format.money(q.grandTotal()));
        draw(document, "amountInWords", " "+format.amountInWords(q.grandTotal()));
        draw(document, "authorizedPerson", value(e.authorizedPerson(), "Hussain Golwala"));

        draw(document, "hirerDefinition", "\"Hirer/You/Your\": Refers to the person, firm, or company i.e. (M/s. "
                + q.partyName() + ") to whom the scaffolding material is being hired.");
        int minimumDays = e.minimumHireDays() == null ? 90 : e.minimumHireDays();
        draw(document, "minimumHireSentence", "The minimum hire period is " + minimumDays + " days. If the materials is returned before "
                + minimumDays + " days, you will still be charged for the full " + minimumDays + " days of rent.");
        drawReplacementMaterials(document, items);
        draw(document, "securityDepositSentence", "A refundable security deposit of Rs. " + format.quantity(q.securityDeposit())
                + "/- (Rupees " + format.amountInWords(q.securityDeposit()).replace("INR ", "").replace(" Only", "") + " Only). This amount will be");
        draw(document, "advanceRent", format.quantity(e.advanceRent()) + "/-");
        int dueDays = e.paymentDueDays() == null ? 3 : e.paymentDueDays();
        draw(document, "paymentTerms", "We will raise an invoice on a monthly basis. Payment is due within " + dueDays
                + " days from the date of the invoice. If there is any delay in making the payment beyond " + dueDays
                + " (" + format.integerWords(dueDays) + ") days, this matter shall be discussed mutually.");

        draw(document, "signatoryName", value(e.authorizedPerson(), "Hussain Golwala"));
        draw(document, "signatoryDesignation", "(" + value(e.authorizedDesignation(), "Sales Executive") + ")");
        draw(document, "signatoryPhone", " "+value(e.authorizedPhone(), "8451044007"));
        draw(document, "acceptedCustomer", q.partyName());
        draw(document, "acceptedBy", e.acceptedBy());
        draw(document, "acceptedDesignation", e.acceptedDesignation());
        draw(document, "acceptedPhone", e.acceptedPhone());
        draw(document, "acceptedDate", format.date(e.acceptedDate()));
    }

    private void draw(PDDocument document, String key, String value) throws IOException {
        if (value == null || value.isBlank()) return;
        List<FieldArea> areas = config.fields().get(key);
        if (areas == null) throw new IllegalStateException("Missing PDF coordinate field: " + key);
        for (FieldArea area : areas) drawArea(document, area, value);
    }

    private void drawRequiredMaterials(PDDocument document, List<QuotationItemResponse> items) throws IOException {
        fillWhite(document, 0, 43, 546, 330, 111);
        int visible = Math.min(items.size(), 7);
        for (int index = 0; index < visible; index++) {
            QuotationItemResponse item = items.get(index);
            String text = item.itemNameSnapshot() + " - " + format.quantity(item.requiredQuantity()) + " " + unit(item.unitSnapshot());
            drawFittedText(document, 0, 45, 550 + index * 14.5f, 320, 12, 9, "LEFT", false, text);
        }
        if (items.size() > visible) {
            drawFittedText(document, 0, 45, 550 + 6 * 14.5f, 320, 12, 8, "LEFT", true,
                    "+ " + (items.size() - 6) + " additional materials listed in the attached schedule.");
        }
    }

    private void drawPrimaryMaterialTable(PDDocument document, List<QuotationItemResponse> items) throws IOException {
        float[] widths = {48, 84, 48, 48, 47, 48, 96};
        float x = 82, top = 222.5f, rowHeight = 15.5f;
        fillWhite(document, 1, x, top, sum(widths), rowHeight * PRIMARY_TABLE_ROWS);
        for (int row = 0; row < PRIMARY_TABLE_ROWS; row++) {
            if (row < items.size()) drawMaterialRow(document, 1, x, top + row * rowHeight, widths, rowHeight, row + 1, items.get(row));
        }
        drawGrid(document, 1, x, top, widths, rowHeight, PRIMARY_TABLE_ROWS, 0);
    }

    private void appendContinuationSchedules(PDDocument document, PDDocument template,
            List<QuotationItemResponse> items) throws IOException {
        if (items.size() <= PRIMARY_TABLE_ROWS) return;
        List<QuotationItemResponse> remaining = items.subList(PRIMARY_TABLE_ROWS, items.size());
        for (int offset = 0; offset < remaining.size(); offset += CONTINUATION_TABLE_ROWS) {
            int end = Math.min(offset + CONTINUATION_TABLE_ROWS, remaining.size());
            document.importPage(template.getPage(1));
            int pageIndex = document.getNumberOfPages() - 1;
            fillWhite(document, pageIndex, 45, 135, 505, 670);
            drawFittedText(document, pageIndex, 56, 152, 483, 22, 13, "CENTER", true, "MATERIAL SCHEDULE - CONTINUED");
            drawFittedText(document, pageIndex, 56, 177, 483, 16, 9, "LEFT", false,
                    "Additional materials forming part of the same SteelFab hire quotation.");
            drawContinuationTable(document, pageIndex, remaining.subList(offset, end), PRIMARY_TABLE_ROWS + offset + 1);
        }
    }

    private void drawContinuationTable(PDDocument document, int pageIndex, List<QuotationItemResponse> items, int firstSerial) throws IOException {
        float[] widths = {35, 145, 55, 50, 65, 55, 100};
        String[] headers = {"Sr.No.", "Items", "Qty", "Unit", "Rate / Month", "Months", "Total Amount Rs."};
        float x = 56, top = 205, headerHeight = 36, rowHeight = 22;
        for (int column = 0; column < headers.length; column++) {
            drawFittedText(document, pageIndex, x + before(widths, column) + 2, top + 7,
                    widths[column] - 4, 24, 8, "CENTER", true, headers[column]);
        }
        for (int row = 0; row < items.size(); row++) {
            drawMaterialRow(document, pageIndex, x, top + headerHeight + row * rowHeight, widths, rowHeight,
                    firstSerial + row, items.get(row));
        }
        drawGrid(document, pageIndex, x, top, widths, rowHeight, items.size(), headerHeight);
    }

    private void drawMaterialRow(PDDocument document, int pageIndex, float x, float top, float[] widths,
            float rowHeight, int serial, QuotationItemResponse item) throws IOException {
        String[] values = {String.valueOf(serial), item.itemNameSnapshot(), format.quantity(item.quantity()),
                unit(item.unitSnapshot()), format.money(item.rate()), format.quantity(item.hireMonths()), format.money(item.amount())};
        String[] alignments = {"CENTER", "LEFT", "CENTER", "CENTER", "RIGHT", "CENTER", "RIGHT"};
        for (int column = 0; column < values.length; column++) {
            drawFittedText(document, pageIndex, x + before(widths, column) + 2, top + 2,
                    widths[column] - 4, rowHeight - 3, Math.min(8, rowHeight - 5), alignments[column], column == 0, values[column]);
        }
    }

    private void drawReplacementMaterials(PDDocument document, List<QuotationItemResponse> items) throws IOException {
        fillWhite(document, 3, 75, 270, 360, 92);
        int visible = Math.min(items.size(), 6);
        for (int index = 0; index < visible; index++) {
            QuotationItemResponse item = items.get(index);
            BigDecimal rate = item.replacementRate() == null ? BigDecimal.ZERO : item.replacementRate();
            String text = item.itemNameSnapshot() + " - Rs. " + rate.stripTrailingZeros().toPlainString() + "/- per " + unit(item.unitSnapshot()).toLowerCase();
            drawFittedText(document, 3, 82, 273 + index * 14.5f, 345, 12, 8.5f, "LEFT", false, text);
        }
        if (items.size() > visible) {
            drawFittedText(document, 3, 82, 273 + 5 * 14.5f, 345, 12, 8, "LEFT", true,
                    "Additional replacement rates are listed in the material schedule annexure.");
        }
    }

    private void fillWhite(PDDocument document, int pageIndex, float x, float top, float width, float height) throws IOException {
        PDPage page = document.getPage(pageIndex);
        float y = page.getMediaBox().getHeight() - top - height;
        try (PDPageContentStream stream = new PDPageContentStream(document, page,
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            stream.setNonStrokingColor(255, 255, 255);
            stream.addRect(x, y, width, height);
            stream.fill();
        }
    }

    private void drawGrid(PDDocument document, int pageIndex, float x, float top, float[] widths,
            float rowHeight, int rows, float headerHeight) throws IOException {
        PDPage page = document.getPage(pageIndex);
        float totalHeight = headerHeight + rows * rowHeight;
        float yTop = page.getMediaBox().getHeight() - top;
        try (PDPageContentStream stream = new PDPageContentStream(document, page,
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            stream.setStrokingColor(45, 45, 45);
            stream.setLineWidth(.45f);
            stream.addRect(x, yTop - totalHeight, sum(widths), totalHeight);
            float currentX = x;
            for (int column = 0; column < widths.length - 1; column++) {
                currentX += widths[column]; stream.moveTo(currentX, yTop); stream.lineTo(currentX, yTop - totalHeight);
            }
            if (headerHeight > 0) { stream.moveTo(x, yTop - headerHeight); stream.lineTo(x + sum(widths), yTop - headerHeight); }
            for (int row = 1; row < rows; row++) {
                float y = yTop - headerHeight - row * rowHeight; stream.moveTo(x, y); stream.lineTo(x + sum(widths), y);
            }
            stream.stroke();
        }
    }

    private void drawFittedText(PDDocument document, int pageIndex, float x, float top, float width, float height,
            float preferredSize, String align, boolean bold, String rawValue) throws IOException {
        if (rawValue == null || rawValue.isBlank()) return;
        PDFont font = bold ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
        String value = rawValue.trim(); float size = preferredSize;
        while (size > 5.5f && font.getStringWidth(value) / 1000f * size > width) size -= .5f;
        while (font.getStringWidth(value) / 1000f * size > width && value.length() > 4) value = value.substring(0, value.length() - 4) + "...";
        PDPage page = document.getPage(pageIndex);
        float textWidth = font.getStringWidth(value) / 1000f * size;
        float textX = switch (align) {
            case "RIGHT" -> x + width - textWidth;
            case "CENTER" -> x + (width - textWidth) / 2f;
            default -> x;
        };
        try (PDPageContentStream stream = new PDPageContentStream(document, page,
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            stream.setNonStrokingColor(0, 0, 0); stream.beginText(); stream.setFont(font, size);
            stream.newLineAtOffset(textX, page.getMediaBox().getHeight() - top - Math.min(size, height));
            stream.showText(value); stream.endText();
        }
    }

    private float before(float[] values, int index) { float total = 0; for (int i = 0; i < index; i++) total += values[i]; return total; }
    private float sum(float[] values) { float total = 0; for (float value : values) total += value; return total; }

    private void drawArea(PDDocument document, FieldArea area, String value) throws IOException {
        PDPage page = document.getPage(area.page() - 1);
        float pageHeight = page.getMediaBox().getHeight();
        float y = pageHeight - area.top() - area.height();
        PDFont font = Boolean.TRUE.equals(area.bold()) ? PDType1Font.HELVETICA_BOLD : PDType1Font.HELVETICA;
        try (PDPageContentStream stream = new PDPageContentStream(document, page,
                PDPageContentStream.AppendMode.APPEND, true, true)) {
            stream.setNonStrokingColor(255, 255, 255);
            stream.addRect(area.x(), y, area.width(), area.height());
            stream.fill();
            stream.setNonStrokingColor(0, 0, 0);
            float fontSize = area.fontSize();
            List<String> lines = fittedLines(font, area, value, fontSize);
            float lineHeight = fontSize * 1.2f;
            while ((lines.size() * lineHeight > area.height() + 1 || widest(font, fontSize, lines) > area.width() + .5f)
                    && fontSize > 6f) {
                fontSize -= .5f;
                lines = fittedLines(font, area, value, fontSize);
                lineHeight = fontSize * 1.2f;
            }
            if (lines.size() * lineHeight > area.height() + 1 || widest(font, fontSize, lines) > area.width() + .5f) overflow(value);
            for (int index = 0; index < lines.size(); index++) {
                String line = lines.get(index);
                float textWidth = font.getStringWidth(line) / 1000f * fontSize;
                float x = switch (value(area.align(), "LEFT")) {
                    case "RIGHT" -> area.x() + area.width() - textWidth;
                    case "CENTER" -> area.x() + (area.width() - textWidth) / 2f;
                    default -> area.x();
                };
                stream.beginText();
                stream.setFont(font, fontSize);
                stream.newLineAtOffset(x, pageHeight - area.top() - fontSize - index * lineHeight);
                stream.showText(line);
                stream.endText();
            }
        }
    }

    private List<String> fittedLines(PDFont font, FieldArea area, String value, float fontSize) throws IOException {
        return Boolean.TRUE.equals(area.wrap()) ? wrap(font, fontSize, area.width(), value) : List.of(value);
    }

    private float widest(PDFont font, float fontSize, List<String> lines) throws IOException {
        float widest = 0;
        for (String line : lines) widest = Math.max(widest, font.getStringWidth(line) / 1000f * fontSize);
        return widest;
    }

    private List<String> wrap(PDFont font, float size, float width, String value) throws IOException {
        List<String> lines = new ArrayList<>(); StringBuilder line = new StringBuilder();
        for (String word : value.trim().split("\\s+")) {
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (font.getStringWidth(candidate) / 1000f * size <= width) line.replace(0, line.length(), candidate);
            else { if (line.isEmpty()) overflow(word); lines.add(line.toString()); line.replace(0, line.length(), word); }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private void requireExactTemplate(QuotationResponse quotation) {
        if (!TEMPLATE_CODE.equals(quotation.quotationTemplateCode()))
            throw new BusinessRuleException("WRONG_PDF_TEMPLATE", "Quotation does not use the SteelFab exact hire template");
    }
    private void overflow(String value) { throw new BusinessRuleException("EXACT_PDF_TEXT_OVERFLOW", "Value is too long for the exact PDF: " + value); }
    private String unit(String unit) { return unit == null || unit.isBlank() ? "Nos." : unit; }
    private String value(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private String filename(QuotationResponse q) { return "steelfab-hire-" + q.quotationNumber().replaceAll("[^A-Za-z0-9.-]", "-") + ".pdf"; }

    public record PdfDocument(String filename, byte[] content, int templateVersion, int coordinatesVersion) {}
    public record ExactPdfConfig(String templateCode, int templateVersion, int coordinatesVersion, int pageCount,
            float pageHeight, Map<String, List<FieldArea>> fields) {}
    public record FieldArea(int page, float x, float top, float width, float height, float fontSize,
            String align, Boolean bold, Boolean wrap, Integer rows, Float rowHeight) {}
}
