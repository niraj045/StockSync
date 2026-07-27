package com.stocksync.exception.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.stocksync.exception.dto.SiteTransferResponse;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Service
public class SiteTransferPdfService {
    private final SpringTemplateEngine templates;

    public SiteTransferPdfService(SpringTemplateEngine templates) {
        this.templates = templates;
    }

    public PdfDocument generate(SiteTransferResponse transfer) {
        Context context = new Context();
        context.setVariables(Map.of("t", transfer));
        String html = templates.process("site-transfer-pdf", context);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            new PdfRendererBuilder().withHtmlContent(html, null).toStream(output).run();
            String filename = "transfer-" + transfer.transferNumber().replaceAll("[^A-Za-z0-9.-]", "-") + ".pdf";
            return new PdfDocument(filename, output.toByteArray());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate transfer PDF", e);
        }
    }

    public record PdfDocument(String filename, byte[] content) {}
}
