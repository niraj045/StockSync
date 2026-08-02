package com.stocksync.order.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.stocksync.common.pdf.PdfBranding;
import com.stocksync.order.dto.OrderResponse;
import java.io.*;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class OrderPdfService {
    private final SpringTemplateEngine templates;
    private final PdfBranding branding;

    public OrderPdfService(SpringTemplateEngine templates, PdfBranding branding) {
        this.templates = templates;
        this.branding = branding;
    }

    public PdfDocument generate(OrderResponse order) {
        Context context = new Context();
        context.setVariables(Map.of(
                "o", order,
                "companyName", branding.companyName(),
                "letterheadDataUri", branding.letterheadDataUri()));
        String html = templates.process("order-pdf", context);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new PdfRendererBuilder().withHtmlContent(html, null).toStream(output).run();
            String filename = "order-" + order.orderNumber().replaceAll("[^A-Za-z0-9.-]", "-") + ".pdf";
            return new PdfDocument(filename, output.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate order PDF", e);
        }
    }

    public record PdfDocument(String filename, byte[] content) {}
}
