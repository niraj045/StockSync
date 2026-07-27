package com.stocksync.billing.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.stocksync.billing.dto.InvoiceDtos.InvoiceResponse;
import java.io.*;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class InvoicePdfService {
    private final SpringTemplateEngine templates;

    public InvoicePdfService(SpringTemplateEngine templates) {
        this.templates = templates;
    }

    public byte[] generate(InvoiceResponse invoice) {
        Context context = new Context();
        context.setVariables(Map.of("inv", invoice));
        String html = templates.process("invoice-pdf", context);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new PdfRendererBuilder().withHtmlContent(html, null).toStream(output).run();
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate invoice PDF", e);
        }
    }
}
