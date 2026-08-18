package com.stocksync.billing.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.stocksync.billing.dto.InvoiceDtos.InvoiceItemResponse;
import com.stocksync.billing.dto.InvoiceDtos.InvoiceResponse;
import com.stocksync.common.pdf.PdfBranding;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

class InvoicePdfServiceTest {

    @Test
    void rendersTheApprovedClientInvoiceFormatOnLetterPaper() throws Exception {
        InvoicePdfService service = new InvoicePdfService(templateEngine(), new PdfBranding());

        byte[] bytes = service.generate(invoice());

        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(bytes))) {
            String text = new PDFTextStripper().getText(document);
            assertEquals(1, document.getNumberOfPages());
            assertEquals(612f, document.getPage(0).getMediaBox().getWidth(), 0.5f);
            assertEquals(792f, document.getPage(0).getMediaBox().getHeight(), 0.5f);
            assertTrue(text.contains("Tax Invoice"));
            assertTrue(text.contains("INV/2026-27/0036"));
            assertTrue(text.contains("4m Facade LLP"));
            assertTrue(text.contains("Supply of Scaffolding Material on Hire"));
            assertTrue(text.contains("Net Hire Charges Rs."));
            assertTrue(text.contains("8,340.00"));
            assertTrue(text.contains("Gross Hire Charges Rs."));
            assertTrue(text.contains("9,841.20"));
            assertTrue(text.contains("BANK DETAILS"));
            var xObjects = document.getPage(0).getResources().getXObjectNames().iterator();
            assertTrue(xObjects.hasNext(), "The invoice must contain the letterhead image");
            xObjects.next();
            assertTrue(xObjects.hasNext(), "The invoice must contain the company stamp image");
        }
    }

    private SpringTemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private InvoiceResponse invoice() {
        InvoiceItemResponse line = new InvoiceItemResponse(
                1L, "RENTAL", 10L, "ISSUED_CHALLAN", 20L, "734/26-27", 30L,
                "HF", "H frames", null, "NOS", "Monthly rental", new BigDecimal("33"),
                null, null, 60, new BigDecimal("60"), true, new BigDecimal("8340"), 1
        );
        return new InvoiceResponse(
                1L, "INV/2026-27/0036", 2L, "BR/2026-27/0036", 3L, "AGR/2026-27/0010",
                "QUANTITY", "SteelFab Scaffoldings & Engineering Pvt. Ltd.", "Mumbai",
                "27AANCS6797C1ZR", "4m Facade LLP", "27AACFZ3323M1ZW",
                "Gala no. 4c, old Anjirwadi, Mumbai", "Maharashtra", "Juhu", "JH-01",
                "Juhu, Andheri West, Mumbai", null, LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 4), LocalDate.of(2026, 4, 28), LocalDate.of(2026, 6, 27),
                "DRAFT", new BigDecimal("8340.00"), BigDecimal.ZERO, new BigDecimal("8340.00"),
                new BigDecimal("9"), new BigDecimal("750.60"), new BigDecimal("9"),
                new BigDecimal("750.60"), BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("1501.20"), BigDecimal.ZERO, new BigDecimal("9841.20"),
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("9841.20"),
                "UNPAID", null, null, null, null, null, null, null, null, 0L, List.of(line)
        );
    }
}
