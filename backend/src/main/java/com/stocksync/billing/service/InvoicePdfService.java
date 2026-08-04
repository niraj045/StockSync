package com.stocksync.billing.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.stocksync.billing.dto.InvoiceDtos.InvoiceResponse;
import com.stocksync.common.pdf.PdfBranding;
import com.stocksync.common.pdf.PdfViewHelper;
import java.io.*;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class InvoicePdfService {
    private final SpringTemplateEngine templates;
    private final PdfBranding branding;

    public InvoicePdfService(SpringTemplateEngine templates, PdfBranding branding) {
        this.templates = templates;
        this.branding = branding;
    }

    public byte[] generate(InvoiceResponse invoice) {
        Context context = new Context();
        context.setVariables(Map.of("inv", invoice, "fmt", PdfViewHelper.INSTANCE));
        String html = templates.process("invoice-pdf", context);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new PdfRendererBuilder().useFastMode().withHtmlContent(html, null).toStream(output).run();
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate invoice PDF", e);
        }
    }
}
